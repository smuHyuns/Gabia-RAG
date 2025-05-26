package gabia.internship.god.integration.search.service.qdrant;

import gabia.internship.god.embedding.dto.response.openAI.EmbeddingResponseDto;
import gabia.internship.god.integration.base.QdrantBaseIntegrationTest;
import gabia.internship.god.search.dto.response.qdrant.QdrantSearchResultDto;
import gabia.internship.god.search.service.QdrantSearchService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import static gabia.internship.god.integration.search.service.qdrant.QdrantSearchServiceUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class QdrantSearchServiceIntTest extends QdrantBaseIntegrationTest {

    @Autowired
    private QdrantSearchService qdrantSearchService;

    @DisplayName("Qdrant 유사도 문서 탐색 성공 테스트")
    @Test
    void searchDocsForQuestion_success() {
        // given
        EmbeddingResponseDto requestDto = makeMockEmbeddingResponse();

        // when
        Mono<QdrantSearchResultDto> resultMono = qdrantSearchService.searchDocsForQuestion(requestDto, COLLECTION_NAME);

        // then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertNotNull(result);
                    assertFalse(result.result().isEmpty());
                })
                .verifyComplete();
    }

    @DisplayName("Qdrant 유사도 문서 탐색 실패 테스트 - 결과 없음")
    @Test
    void searchDocsForQuestion_noResult() {
        // given - 유효하지 않은 embeddingDto 생성
        EmbeddingResponseDto requestDto = makeInvalidMockEmbeddingResponse();

        // when
        Mono<QdrantSearchResultDto> resultMono = qdrantSearchService.searchDocsForQuestion(requestDto, COLLECTION_NAME);

        // then
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Qdrant API 호출 실패"))
                .verify();
    }
}
