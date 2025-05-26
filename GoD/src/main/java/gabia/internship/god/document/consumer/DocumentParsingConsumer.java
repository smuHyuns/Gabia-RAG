package gabia.internship.god.document.consumer;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.config.webflux.ConcurrencyProperties;
import gabia.internship.god.common.handler.MessageHandler;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.common.message.JsonToParsingMessage;
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
public class DocumentParsingConsumer {
    private final MessageHandler<JsonToParsingMessage> handler;
    private final Receiver receiver;
    private final RabbitMQProperties props;
    private final ConcurrencyProperties concurrency;
    private final UploadStatusService statusService;

    @EventListener(ApplicationReadyEvent.class)
    public void listenDocumentParsingMessage() {
        receiver.consumeManualAck(props.queue().workDocumentParsing())
                .flatMap(receivedMessage -> {
                    int retryCount = statusService.getRetryCount(receivedMessage.getProperties().getHeaders());
                    return Mono.fromCallable(() ->
                                    JsonMessageConverter.fromBytes(receivedMessage.getBody(), JsonToParsingMessage.class)
                            )
                            .flatMap(message ->
                                    handler.handle(message, retryCount)
                                            .doOnSuccess(res -> log.info("파싱 임베딩 발송 처리 성공 - ACK"))
                                            .then(Mono.fromRunnable(receivedMessage::ack))
                                            .onErrorResume(e -> {
                                                receivedMessage.nack(false, false);
                                                log.error("파싱 임베딩 발송 처리 실패 - NACK & DLQ 전송", e);
                                                return Mono.empty();
                                            })
                            )
                            .onErrorResume(e -> {
                                receivedMessage.nack(false, false);
                                log.error("파싱 후 임베딩큐 전송 실패 - NACK & DLQ 전송\n본문: {}", new String(receivedMessage.getBody()), e);
                                return Mono.empty();
                            });
                }, concurrency.parsing())
                .onErrorContinue((e, msg) -> log.error("전체 스트림 예외 무시", e))
                .subscribe();
    }
}
