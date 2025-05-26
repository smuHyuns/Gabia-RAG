package gabia.internship.god.core.vectorstore;

import reactor.core.publisher.Mono;

public interface VectorStoreSearchService<M> {
    // vectorstore의 dataSet에서 전달받은 embedding값과 유사한 값들을 탐색합니다.
    Mono<Void> searchDocsForQuestion(M message);
}
