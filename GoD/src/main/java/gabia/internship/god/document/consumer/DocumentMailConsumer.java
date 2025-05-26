package gabia.internship.god.document.consumer;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.config.webflux.ConcurrencyProperties;
import gabia.internship.god.common.handler.MessageHandler;
import gabia.internship.god.common.message.DocumentToMailMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
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
public class DocumentMailConsumer {
    private final MessageHandler<DocumentToMailMessage> handler;
    private final Receiver receiver;
    private final ConcurrencyProperties concurrency;
    private final RabbitMQProperties props;

    @EventListener(ApplicationReadyEvent.class)
    public void listenDocumentMailMessage() {
        receiver.consumeManualAck(props.queue().workDocumentMail())
                .flatMap(receivedMessage ->
                        Mono.fromCallable(() ->
                                        JsonMessageConverter.fromBytes(receivedMessage.getBody(), DocumentToMailMessage.class)
                                )
                                .flatMap(message ->
                                        handler.handle(message, null)
                                                .doOnSuccess(res -> log.info("메일 발송 처리 성공 - ACK"))
                                                .then(Mono.fromRunnable(receivedMessage::ack))
                                                .onErrorResume(e -> {
                                                    receivedMessage.nack(false, false);
                                                    log.error("메일 발송 처리 실패 - NACK & DLQ 전송", e);
                                                    return Mono.empty();
                                                })
                                )
                                .onErrorResume(e -> {
                                    receivedMessage.nack(false, false);
                                    log.error("메일 발송 실패 - NACK & DLQ 전송\n본문: {}", new String(receivedMessage.getBody()), e);
                                    return Mono.empty();
                                }), concurrency.documentMail())
                .onErrorContinue((e, msg) -> log.error("전체 스트림 예외 무시", e))
                .subscribe();
    }
}