package gabia.internship.god.common.handler;

import reactor.core.publisher.Mono;

public interface MessageHandler<M> {
    /**
     * NOTE: retryCount가 사용되지 않는 Handler는 null을 매개변수로써 넣어 사용합니다.
     */
    Mono<Void> handle(M message, Integer retryCount);
}
