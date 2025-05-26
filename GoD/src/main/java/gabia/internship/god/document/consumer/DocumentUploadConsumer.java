package gabia.internship.god.document.consumer;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.config.webflux.ConcurrencyProperties;
import gabia.internship.god.common.handler.MessageHandler;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.common.message.EmbeddingToUploadMessage;
import gabia.internship.god.document.service.UploadStatusService;
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
public class DocumentUploadConsumer {
    private final MessageHandler<EmbeddingToUploadMessage> handler;
    private final UploadStatusService statusService;
    private final RabbitMQProperties props;
    private final ConcurrencyProperties concurrency;
    private final Receiver receiver;

    @EventListener(ApplicationReadyEvent.class)
    public void listenDocumentUploadMessage() {
        receiver.consumeManualAck(props.queue().workDocument())
                .flatMap(receivedMessage -> {
                    int retryCount = statusService.getRetryCount(receivedMessage.getProperties().getHeaders());
                    return Mono.fromCallable(() ->
                            JsonMessageConverter.fromBytes(receivedMessage.getBody(), EmbeddingToUploadMessage.class)
                    ).flatMap(message ->
                            handler.handle(message, retryCount)
                                    .doOnSuccess(ignored -> log.info("문서 업로드 성공 - ACK"))
                                    .then(Mono.fromRunnable(receivedMessage::ack))
                                    .onErrorResume(e -> {
                                        receivedMessage.nack(false, false);
                                        log.error("문서 업로드 실패 - NACK & DLQ 전송", e);
                                        return Mono.empty();
                                    })
                    ).onErrorResume(e -> {
                        receivedMessage.nack(false, false);
                        log.error("벡터 DB 업로드 실패 - NACK & DLQ 전송\n본문: {}", new String(receivedMessage.getBody()), e);
                        return Mono.empty();
                    });
                }, concurrency.document())
                .onErrorContinue((e, msg) -> log.error("전체 스트림 예외 무시", e))
                .subscribe();
    }

}
