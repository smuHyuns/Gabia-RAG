package gabia.internship.god.embedding.consumer;

import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.handler.MessageHandler;
import gabia.internship.god.common.message.QuestionToEmbeddingMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
import gabia.internship.god.core.llm.LLMEmbeddingService;
import gabia.internship.god.embedding.dto.response.openAI.EmbeddingResponseDto;
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
public class EmbeddingConsumer {

    private final Receiver receiver;

    private final RabbitMQProperties props;

    private final MessageHandler<QuestionToEmbeddingMessage> handler;

    @EventListener(ApplicationReadyEvent.class)
    public void listenEmbeddingMessage() {
        receiver.consumeManualAck(props.queue().workEmbedding())
                .flatMap(receivedMessage ->
                        Mono.fromCallable(() ->
                                        JsonMessageConverter.fromBytes(receivedMessage.getBody(), QuestionToEmbeddingMessage.class)
                                )
                                .flatMap(message ->
                                        handler.handle(message, null)
                                                .doOnSuccess(res -> log.info("임베딩 처리 성공 - ACK"))
                                                .then(Mono.fromRunnable(receivedMessage::ack))
                                                .onErrorResume(e -> {
                                                    receivedMessage.nack(false, false);
                                                    log.error("임베딩 처리 실패 - NACK & DLQ 전송", e);
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
