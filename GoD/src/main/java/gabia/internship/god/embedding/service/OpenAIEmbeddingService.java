package gabia.internship.god.embedding.service;

import gabia.internship.god.common.config.llm.LLMProperties;
import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.exception.CommonException;
import gabia.internship.god.common.message.EmbeddingToSearchMessage;
import gabia.internship.god.common.message.QuestionToEmbeddingMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.core.llm.LLMEmbeddingService;
import gabia.internship.god.embedding.dto.request.openAI.EmbeddingRequestDto;
import gabia.internship.god.common.constants.Constants;
import gabia.internship.god.embedding.dto.response.openAI.EmbeddingResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIEmbeddingService implements LLMEmbeddingService<EmbeddingResponseDto, String, QuestionToEmbeddingMessage> {

    private final WebClient llmWebClient;

    private final Sender sender;

    private final RabbitMQProperties rabbitProps;

    private final LLMProperties llmProps;

    @Override
    public Mono<EmbeddingResponseDto> embeddingData(List<String> inputs) {

        return llmWebClient.post()
                .uri(Constants.OPENAI_EMBEDDING_PATH)
                .bodyValue(EmbeddingRequestDto.makeEmbeddingRequest(llmProps.model().embedding(), inputs))
                .retrieve()
                .bodyToMono(EmbeddingResponseDto.class)
                .doOnError(e -> log.error("임베딩 API 예외 발생 - {}", e.getMessage(), e))
                .onErrorMap(e -> {
                    if (e instanceof WebClientResponseException) {
                        return new CommonException("OpenAI 호출 실패", e, HttpStatus.BAD_GATEWAY);
                    } else if (e instanceof HttpMessageNotReadableException) {
                        return new CommonException("응답 역직렬화 실패", e, HttpStatus.BAD_REQUEST);
                    } else {
                        return new CommonException("예상치 못한 에러", e, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                });
    }

    @Override
    public Mono<Void> embeddingQuestion(QuestionToEmbeddingMessage message) {
        return llmWebClient.post()
                .uri(Constants.OPENAI_EMBEDDING_PATH)
                .bodyValue(EmbeddingRequestDto.makeEmbeddingRequest(llmProps.model().embedding(), List.of(message.question())))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                            log.error("OpenAI API 응답 실패 - 질문 임베딩");
                            return Mono.error(new RuntimeException("질문 임베딩, OpenAI API 호출 실패"));
                        }
                )
                .bodyToMono(EmbeddingResponseDto.class)
                .flatMap(response -> {
                            EmbeddingToSearchMessage next = new EmbeddingToSearchMessage(
                                    message.userId(),
                                    message.type(),
                                    message.question(),
                                    response.data().get(0).embedding()
                            );
                            byte[] body = JsonMessageConverter.toBytes(next);

                            OutboundMessage outbound = new OutboundMessage(
                                    rabbitProps.exchange().main(),
                                    rabbitProps.routing().workSearch(),
                                    body
                            );

                            return sender.send(Mono.just(outbound));
                        }
                ).doOnSuccess(success -> log.info("RabbitMQ 전송 성공, embedding -> search, {}, {}, {}", message.userId(), message.type().getType(), message.question()))
                .doOnError(e -> log.error("임베딩 API 예외 발생, {}", e.getMessage()))
                .onErrorMap(e -> {
                    if (e instanceof WebClientResponseException) {
                        return new CommonException("OpenAI 호출 실패", e, HttpStatus.BAD_GATEWAY);
                    } else if (e instanceof HttpMessageNotReadableException) {
                        return new CommonException("응답 역직렬화 실패", e, HttpStatus.BAD_REQUEST);
                    } else {
                        return new CommonException("예상치 못한 에러", e, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }).then();
    }

}
