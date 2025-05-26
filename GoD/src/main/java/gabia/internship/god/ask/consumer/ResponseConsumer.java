package gabia.internship.god.ask.consumer;

import gabia.internship.god.ask.service.AskService;
import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.message.GenerateToResponseMessage;
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
public class ResponseConsumer {

    private final Receiver receiver;

    private final RabbitMQProperties rabbitProps;

    private final AskService askService;

    @EventListener(ApplicationReadyEvent.class)
    public void listenResponseMessage() {
        receiver.consumeManualAck(rabbitProps.queue().workResponse())
                .flatMap(receivedMessage ->
                        Mono.fromCallable(() ->
                                JsonMessageConverter.fromBytes(receivedMessage.getBody(), GenerateToResponseMessage.class)
                        )
                        .flatMap(message ->
                                askService.consumeResponseMessage(message)
                                        .doOnSuccess(res -> log.info("응답 처리 성공 - ACK"))
                                        .then(Mono.fromRunnable(receivedMessage::ack))
                                        .onErrorResume(e -> {
                                            receivedMessage.nack(false, false);
                                            log.error("응답 처리 중 예외 발생 - NACK & DLQ 전송", e);
                                            return Mono.empty();
                                        })
                        )
                        .onErrorResume(e -> {
                            // 역직렬화 실패 등: 구조 자체가 잘못됨 → NACK
                            receivedMessage.nack(false, false);
                            log.error("역직렬화 실패 - NACK & DLQ 전송\n본문: {}", new String(receivedMessage.getBody()), e);
                            return Mono.empty();
                        }))
                .onErrorContinue((e, msg) -> log.error("전체 스트림 예외 무시", e))
                .subscribe();
    }

}
