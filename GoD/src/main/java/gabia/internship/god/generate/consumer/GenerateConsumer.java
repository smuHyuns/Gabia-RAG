package gabia.internship.god.generate.consumer;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.message.SearchToGenerateMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.core.llm.LLMGenerateService;
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
public class GenerateConsumer {

    private final Receiver receiver;

    private final RabbitMQProperties rabbitProps;

    private final LLMGenerateService<SearchToGenerateMessage> openAIGenerateService;

    @EventListener(ApplicationReadyEvent.class)
    public void listenGenerateMessage() {
        receiver.consumeManualAck(rabbitProps.queue().workGenerate())
                .flatMap(receivedMessage ->
                        Mono.fromCallable(() ->
                                        JsonMessageConverter.fromBytes(receivedMessage.getBody(), SearchToGenerateMessage.class)
                                )
                                .flatMap(message ->
                                        openAIGenerateService.makeResponse(message)
                                                .doOnSuccess(res -> log.info("응답 생성 성공 - ACK"))
                                                .then(Mono.fromRunnable(receivedMessage::ack))
                                                .onErrorResume(e -> {
                                                    receivedMessage.nack(false, false);
                                                    log.error("응답 생성 실패 - NACK & DLQ 전송", e);
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
