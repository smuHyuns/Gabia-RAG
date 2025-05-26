package gabia.internship.god.common.consumer;

import com.rabbitmq.client.LongString;
import gabia.internship.god.common.config.rabbitmq.RabbitMQProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DLQConsumer {

    private final Receiver receiver;

    private final Sender sender;

    private final RabbitMQProperties props;

    @EventListener(ApplicationReadyEvent.class)
    public void consumeDeadQueue() {
        receiver.consumeManualAck(props.queue().dead())
                .flatMap(message -> {
                    Map<String, Object> headers = message.getProperties().getHeaders();
                    long retryCount = getRetryCount(headers);
                    String originalRoutingKey = getOriginalRoutingKey(headers);
                    String routingKey = (originalRoutingKey != null && originalRoutingKey.startsWith("q."))
                            ? originalRoutingKey.substring(2)
                            : originalRoutingKey;

                    log.warn("DLQ 메시지 수신 - retryCount={}, routingKey={}, headers={}", retryCount, routingKey, headers);

                    if (retryCount <= 3 && routingKey != null) {
                        return sender.send(Mono.just(new OutboundMessage(
                                        props.exchange().main(),
                                        routingKey,
                                        message.getProperties(),   // 기존 헤더 유지 → x-death 누적
                                        message.getBody()
                                )))
                                .doOnSuccess(i -> log.info("✅ DLQ 재전송 성공: routingKey={}", routingKey))
                                .then(Mono.fromRunnable(message::ack));
                    } else {
                        log.error("DLQ 재시도 불가 (횟수 초과 또는 라우팅 키 없음): {}", new String(message.getBody(), StandardCharsets.UTF_8));
                        return Mono.fromRunnable(message::ack); // 그냥 ack하고 버림
                    }
                })
                .onErrorContinue((e, obj) -> log.error("DLQ 처리 중 예외 발생", e))
                .subscribe();
    }

    @SuppressWarnings("unchecked")
    private long getRetryCount(Map<String, Object> headers) {
        Object xDeath = headers.get("x-death");
        if (!(xDeath instanceof List<?> xDeathList) || xDeathList.isEmpty()) return 0;

        Map<String, Object> latestDeath = (Map<String, Object>) xDeathList.get(0);
        Object count = latestDeath.get("count");

        if (count instanceof Long l) return l;
        try {
            return Long.parseLong(count.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private String getOriginalRoutingKey(Map<String, Object> headers) {
        Object xDeath = headers.get("x-death");
        if (!(xDeath instanceof List<?> xDeathList) || xDeathList.isEmpty()) return null;

        Map<String, Object> latestDeath = (Map<String, Object>) xDeathList.get(0);
        Object routingKeysObj = latestDeath.get("routing-keys");

        if (!(routingKeysObj instanceof List<?> routingKeys) || routingKeys.isEmpty()) return null;

        Object rawKey = routingKeys.get(0);
        if (rawKey == null) return null;

        if (rawKey instanceof LongString longString) {
            return longString.toString(); // 또는 .utf8() → 디코딩 방식 명시하고 싶다면
        }

        return rawKey.toString();
    }

}
