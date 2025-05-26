package gabia.internship.god.core.vectorstore;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VectorStoreDocumentService<DataSetT, VectorT> {

    // 해당 dataSet의 이름을 가진 set이 vectorstore에 존재하는지 확인합니다.
    Mono<Boolean> exists(DataSetT dataSet);

    // 해당 dataSet의 이름을 가진 set을 생성합니다.
    Mono<Void> createDataSet(DataSetT req);

    // 해당 dataSet에 vectorData들을 업로드합니다.
    Flux<Integer> uploadVectors(DataSetT dataSet, Flux<VectorT> vectors);
}
