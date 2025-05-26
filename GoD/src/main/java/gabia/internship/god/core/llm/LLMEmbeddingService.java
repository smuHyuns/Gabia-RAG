package gabia.internship.god.core.llm;

import reactor.core.publisher.Mono;

import java.util.List;

public interface LLMEmbeddingService <Res ,T, M> {
    // 질문(단일 문장)에 대한 임베딩을 실시합니다.
    Mono<Void> embeddingQuestion(M message);

    // 데이터(복수 문장)에 대한 임베딩을 실시합니다.
    Mono<Res> embeddingData(List<T> rows);
}
