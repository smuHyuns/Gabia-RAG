package gabia.internship.god.ask.handler;

import com.fasterxml.jackson.databind.JsonNode;
import gabia.internship.god.ask.dto.request.EQuestionType;
import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import gabia.internship.god.common.message.QuestionToEmbeddingMessage;
import gabia.internship.god.common.util.JsonMessageConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final Sender sender;

    private final RabbitMQProperties props;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String userId = extractUserId(session);
        sessions.put(userId, session);
        log.info("WebSocket 연결됨 - userId={}", userId);

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> handleUserQuestion(payload, userId))
                .doOnError(error -> log.error("WebSocket 처리 중 예외 발생 - userId={}, error={}", userId, error.getMessage(), error))
                .doFinally(signal -> {
                    sessions.remove(userId);
                    log.info("WebSocket 연결 종료됨 - userId={}", userId);
                })
                .then();
    }

    private Mono<Void> handleUserQuestion(String payload, String userId) {
        return Mono.fromCallable(() -> {
                    JsonNode jsonNode = JsonMessageConverter.fromBytes(payload.getBytes(), JsonNode.class);
                    String question = jsonNode.get("question").asText();
                    EQuestionType questionType = EQuestionType.valueOf(jsonNode.get("questionType").asText());

                    return new QuestionToEmbeddingMessage(userId, question, questionType);
                })
                .flatMap(message -> {
                    OutboundMessage outbound = new OutboundMessage(
                            props.exchange().main(),
                            props.routing().workEmbedding(),
                            JsonMessageConverter.toBytes(message)
                    );
                    return sender.send(Mono.just(outbound));
                })
                .doOnSuccess(unused -> log.info("질문 MQ 전송 완료 - userId={}", userId))
                .onErrorResume(e -> {
                    log.error("질문 MQ 전송 실패 - userId={}, 이유={}", userId, e.getMessage(), e);
                    return sendToUser(userId, Map.of("error", "질문 전송에 실패했습니다."));
                });
    }

    public Mono<Void> sendToUser(String userId, Object payload) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonText = JsonMessageConverter.toBytes(payload) != null
                        ? new String(JsonMessageConverter.toBytes(payload))
                        : "{}";
                return session.send(Mono.just(session.textMessage(jsonText)))
                        .doOnSuccess(unused -> log.info("사용자에게 메시지 전송 완료 - userId={}", userId))
                        .doOnError(e -> log.error("사용자 메시지 전송 실패 - userId={}, 이유={}", userId, e.getMessage(), e));
            } catch (Exception e) {
                log.error("메시지 직렬화 실패 - userId={}, payload={}", userId, payload, e);
                return Mono.empty();
            }
        } else {
            log.warn("메시지 전송 시도 실패 - 세션이 닫힘 또는 없음: userId={}", userId);
            return Mono.empty();
        }
    }

    private String extractUserId(WebSocketSession session) {
        URI uri = session.getHandshakeInfo().getUri();
        String query = uri.getQuery(); // 예: userId=abc123
        if (query != null && query.startsWith("userId=")) {
            return query.substring("userId=".length());
        }
        throw new IllegalArgumentException("userId 쿼리 파라미터 누락");
    }
}
