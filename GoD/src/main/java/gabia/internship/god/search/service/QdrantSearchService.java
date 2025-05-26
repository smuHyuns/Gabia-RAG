package gabia.internship.god.search.service;

import gabia.internship.god.ask.dto.request.EQuestionType;
import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.exception.CommonException;
import gabia.internship.god.common.message.EmbeddingToSearchMessage;
import gabia.internship.god.common.message.SearchToGenerateMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.core.vectorstore.VectorStoreSearchService;
import gabia.internship.god.search.dto.response.qdrant.QdrantSearchResultDto;
import gabia.internship.god.search.util.PathUtils;
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

import static gabia.internship.god.search.dto.request.qdrant.QdrantSearchRequest.makeQdrantSearchRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantSearchService implements VectorStoreSearchService<EmbeddingToSearchMessage> {

    private final WebClient vectorStoreWebClient;

    private final Sender sender;

    private final RabbitMQProperties rabbitProps;

    /**
     * doOnNext() VS flatMap()
     * <p>
     * - doOnNext(): 결과에 전혀 영향을 끼치지 않고, 그런 로직만 위치해야 하는 것. 해당 로직에서 예외가 발생해도 사이드 이펙트 없음
     * - flatMap(): 반응형 객체를 평탄화 하여 작업, 작업 완료한 객체를 반응형 객체로 wrapping 가능. -> 루프 체인에 영향이 존재, 예외 발생하면 예외 터짐
     * <p>
     * - MQ는 본 로직의 사이드 이펙트가 없어야 함에 집중했으나, MQ에 대한 전달 실패와 같은 에러 발생 가능
     * -> 여기서 예외가 터지지 않는다면, DLQ로의 이동이나 예외처리가 불가능(예외가 터지지 않기 때문에 컨슈머에서 파악이 불가능하기 때문 - Mono의 스트림에서 처리되지 않기 때문에!!!!!!)
     * -> doOnNext()에서 flatMap()으로 변경하게 된 이유
     * <p>
     * fromRunnable() VS fromCallable()
     * <p>
     * - fromRunnable(): 제시된 작업을 처리하고, 반응형 객체의 .empty()를 반환함.
     * - fromCallable(): 제시된 작업을 처리하고, 반응형 객체 내부에 객체를 넣어서 반환함
     * <p>
     * fromCallable() 선택 이유
     * <p>
     * - Mono.fromRunnable()은 Runnable을 감싸기 때문에, 예외가 발생하면 Mono.error()로 wrapping되지 않고 일반 예외 throw.
     * - 이 경우 Reactor는 이걸 처리하긴 하지만, 디버깅하기 어렵고, 예외 흐름 제어에 취약.
     * <p>
     * <p>
     * Spring WebFlux에서의 예외처리
     * <p>
     * 1. doOnError(Throwable e -> {...})
     * - 흐름에 영향을 주지 않음
     * - 오직 로깅, 모니터링, 알림 전송 등의 부가 작업용
     * <p>
     * 2. onErrorResume(Throwable e -> Mono<T>)
     * - 체인에서 예외를 복구하고 새 흐름으로 전환
     * - 자주 쓰이지만, 예외를 삼켜버리기 때문에 전역 예외 처리(WebExceptionHandler)는 호출되지 않음
     * - 예외를 던지기 위해서 사용되는 함수가 아님!!!!. openAI API의 스트림 응답처럼, 예외가 발생할 상황이지만, 흐름을 깨트리지 않기 위해 사용하는 것이 본래 목적
     * <p>
     * 3. onErrorReturn(T fallback)
     * - 에러 발생 시 정해진 하나의 값을 리턴
     * - 가볍게 fallback 하기에 적절
     * <p>
     * 4. onErrorMap(Throwable e -> new Exception(...))
     * - 예외를 다른 예외로 바꿔서 전파
     * - WebExceptionHandler까지 도달시킬 때 유용
     * - 사실상, 일반적으로 생각하는 예외 던지기는 해당 함수를 사용하는 것이 적절
     * <p>
     * <p>
     * 기존에는 onErrorResume()를 사용해서 예외처리 진행, 물론 동작은 하지만 함수의 본래 목적에 맞지 않는 사용이라고 판단
     * onErrorResume()를 사용해서 에러로 매핑하고, 디테일한 정보 전달을 위해 커스텀 exception을 사용
     */

    @Override
    public Mono<Void> searchDocsForQuestion(EmbeddingToSearchMessage message) {
        return vectorStoreWebClient.post()
                .uri(PathUtils.makeSearchSimilarData(message.type().getType()))
                .bodyValue(makeQdrantSearchRequest(message.values()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    log.error("Qdrant API 응답 실패 - 유사 문서 검색");
                    return Mono.error(new RuntimeException("Qdrant API 호출 실패"));
                })
                .bodyToMono(QdrantSearchResultDto.class)
                .flatMap(response -> {
                    SearchToGenerateMessage body = new SearchToGenerateMessage(
                            message.userId(),
                            message.question(),
                            message.type(),
                            response.result()
                    );

                    OutboundMessage outbound = new OutboundMessage(
                            rabbitProps.exchange().main(),
                            rabbitProps.routing().workGenerate(),
                            JsonMessageConverter.toBytes(body)
                    );
                    return sender.send(Mono.just(outbound));
                })
                .doOnSuccess(unused -> log.info(
                        "검색 결과 성공, 응답 생성 메시지 전달 성공: {}, {}, {}",
                        message.userId(), message.type(), message.question()
                ))
                .doOnError(e -> log.error("데이터 검색 예외 발생, {}", e.getMessage(), e))
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
