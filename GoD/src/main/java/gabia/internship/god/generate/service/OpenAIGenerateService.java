package gabia.internship.god.generate.service;

import gabia.internship.god.common.config.llm.LLMProperties;
import gabia.internship.god.common.config.llm.PromptProperties;
import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.exception.CommonException;
import gabia.internship.god.common.message.GenerateToResponseMessage;
import gabia.internship.god.common.message.SearchToGenerateMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.core.llm.LLMGenerateService;
import gabia.internship.god.generate.dto.request.openAI.ChatRequestDto;
import gabia.internship.god.generate.dto.response.opneAI.AnswerResponseDto;
import gabia.internship.god.common.constants.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import static gabia.internship.god.generate.dto.request.openAI.ChatRequestDto.makeRequestForResponseToLLM;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIGenerateService implements LLMGenerateService<SearchToGenerateMessage> {

    private final WebClient llmWebClient;

    private final Sender sender;

    private final RabbitMQProperties rabbitProps;

    private final LLMProperties llmProps;

    private final PromptProperties promptProps;

    /**
     * flatMap() VS concatMap()
     * <p>
     * 둘 다 루프 체인에서의 작업이라는 점은 동일하지만, 순서 보장의 이유로 concatMap을 선택
     * <p>
     * - flatMap() 병렬 실행 -> 순서 보장이 불가능
     * - concatMap() 한 번에 하나씩 -> 순서 보장
     * <p>
     * 생성된 응답이 stream 방식으로 도달하고, 생성된 순서대로 클라이언트에 전달해야 하기 때문에, concatMap()을 사용해서 응답 chunk의 순서를 맞춰 주는 것이 더 중요하다고 판단
     */
    @Override
    public Mono<Void> makeResponse(SearchToGenerateMessage message) {
        String prompt = promptProps.getPrompt(message.type());
        ChatRequestDto request = makeRequestForResponseToLLM(llmProps.model().generate(), prompt, message.toString());

        return llmWebClient.post()
                .uri(Constants.OPENAI_CHAT_PATH)
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("OpenAI API 응답 실패 - 응답 생성");
                    return Mono.error(new RuntimeException("응답 생성, OpenAI API 호출 실패"));
                })
                .bodyToFlux(AnswerResponseDto.class)
                .concatMap(chunk -> {
                    String content = chunk.choices().get(0).delta().content();
                    GenerateToResponseMessage body = new GenerateToResponseMessage(message.userId(), content);

                    OutboundMessage outbound = new OutboundMessage(
                            rabbitProps.exchange().main(),
                            rabbitProps.routing().workResponse(),
                            JsonMessageConverter.toBytes(body)
                    );

                    return sender.send(Mono.just(outbound))
                            .doOnSuccess(v -> log.info("응답 청크 MQ 전송 완료: {}", content));
                })
                .onErrorResume(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("JsonToken.START_ARRAY")) {
                        // OpenAI API stream 방식 적용, 스트림의 마지막 데이터가 '[DONE]'이기 때문에 역직렬화 실패
                        // 하지만 이는 에러가 아니기 때문에 Mono.empty() 반환
                        return Mono.empty();
                    }
                    return Mono.error(e);
                })
                .doOnError(e -> log.error("응답 생성 에러 발생, {}", e.getMessage()))
                .onErrorMap(e -> {
                    if (e instanceof WebClientResponseException) {
                        return new CommonException("OpenAI 호출 실패", e, HttpStatus.BAD_GATEWAY);
                    } else if (e instanceof HttpMessageNotReadableException) {
                        return new CommonException("응답 역직렬화 실패", e, HttpStatus.BAD_REQUEST);
                    } else {
                        return new CommonException("예상치 못한 에러", e, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                })
                .then();

    }

}
