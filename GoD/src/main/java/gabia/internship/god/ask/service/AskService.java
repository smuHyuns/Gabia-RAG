package gabia.internship.god.ask.service;

import gabia.internship.god.ask.handler.ChatWebSocketHandler;
import gabia.internship.god.common.message.GenerateToResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class AskService {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public Mono<Void> consumeResponseMessage(GenerateToResponseMessage message) {
        log.info("응답 수신: userId={}, chunk={}", message.userId(), message.chunk());
        return chatWebSocketHandler.sendToUser(message.userId(), message.chunk())
                .retry(3)
                .doOnError(e -> log.error("WebSocket 최종 전송 실패 - userId={}, chunk={}", message.userId(), message.chunk(), e));
    }
}

