package gabia.internship.god.core.llm;

import reactor.core.publisher.Mono;

public interface LLMGenerateService<M> {

    Mono<Void> makeResponse(M message);
}
