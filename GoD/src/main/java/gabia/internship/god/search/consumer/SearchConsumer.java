package gabia.internship.god.search.consumer;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.message.EmbeddingToSearchMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.core.vectorstore.VectorStoreSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Receiver;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchConsumer {

    private final Receiver receiver;

    private final RabbitMQProperties rabbitProps;

    private final VectorStoreSearchService<EmbeddingToSearchMessage> qdrantSearchService;

    @EventListener(ApplicationReadyEvent.class)
    public void listenSearchMessage() {
        receiver.consumeManualAck(rabbitProps.queue().workSearch())
                .flatMap(receivedMessage ->
                        Mono.fromCallable(() ->
                                        JsonMessageConverter.fromBytes(receivedMessage.getBody(), EmbeddingToSearchMessage.class)
                        )
                        .flatMap(message ->
                                qdrantSearchService.searchDocsForQuestion(message)
                                        .doOnSuccess(res -> log.info("데이터 검색 성공 - ACK"))
                                        .then(Mono.fromRunnable(receivedMessage::ack))
                                        .onErrorResume(e -> {
                                            receivedMessage.nack(false, false);
                                            log.error("검색 처리 실패 - NACK & DLQ 전송", e);
                                            return Mono.empty();
                                       })
                        )
                        .onErrorResume(e -> {
                            receivedMessage.nack(false, false);
                            log.error("역직렬화 실패 - NACK & DLQ 전송\n본문: {}", new String(receivedMessage.getBody()), e);
                            return Mono.empty();
                        })
                )
                .onErrorContinue((e, msg) -> log.error("전체 스트림 예외 무시", e))
                .subscribe();
    }


}
