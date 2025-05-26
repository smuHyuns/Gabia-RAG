package gabia.internship.god.integration.document.service.qdrant;

import gabia.internship.god.document.service.qdrant.QdrantDocumentService;
import gabia.internship.god.integration.base.QdrantBaseIntegrationTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static gabia.internship.god.integration.document.service.qdrant.QdrantDocumentServiceUtils.createTestFilePart;
import static gabia.internship.god.integration.embedding.service.openai.OpenAIEmbeddingServiceUtils.makeMockEmbeddingResponseDto;

public class QdrantDocumentServiceIntTest extends QdrantBaseIntegrationTest {

    // QdrantBaseIntegrationTest 에서 COLLECTION_NAME에 해당하는 colleciton이 생성, 테스트용 point가 들어가 있는상태
    @Autowired
    private QdrantDocumentService qdrantDocumentService;


    private final String WRONG_COLLECTION_NAME = "this-collection-does-not-exist";

    private final String CSV_CONTENT = """
            doc_id,question,answer
            1,테스트 질문입니다,테스트 콘텐츠 입니다
            """;

    private MockWebServer llmMockWebServer;

    @Value("${port.llm}")
    private Integer llmPort;

    @BeforeAll
    void startMockServer() throws IOException {
        llmMockWebServer = new MockWebServer();
        llmMockWebServer.start(llmPort);
    }

    @AfterAll
    void shutdownMockServer() throws IOException {
        llmMockWebServer.shutdown();
    }


    @Test
    @DisplayName("데이터 저장 테스트 - 정상 작동")
    void saveCsvToDataSet_success() {
        // given
        FilePart filePart = createTestFilePart("test.csv", CSV_CONTENT);

        // OpenAI 임베딩 응답 시뮬레이션 (요청 + 재시도 대비 4개)
        for (int i = 0; i < 4; i++) {
            llmMockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setBody(makeMockEmbeddingResponseDto("text-embedding-3-small"))
                    .addHeader("Content-Type", "application/json"));
        }

        // when
        Mono<Void> result = qdrantDocumentService.saveCsvToDataSet(filePart, COLLECTION_NAME);

        // then
        StepVerifier.create(result)
                .verifyComplete();
    }


    @Test
    @DisplayName("컬렉션 존재 여부 확인 - 존재하지 않을 경우")
    void exists_collectionNotFound() {
        // when
        Mono<Boolean> result = qdrantDocumentService.exists(WRONG_COLLECTION_NAME);

        // then
        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("컬렉션 존재 여부 확인 - 존재하는 경우")
    void exists_collectionExists() {
        // given
        qdrantDocumentService.createDataSet(COLLECTION_NAME).block();

        // when
        Mono<Boolean> result = qdrantDocumentService.exists(COLLECTION_NAME);

        // then
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("createDataSet - 컬렉션이 존재하지 않을 때 생성 성공")
    void createDataSet_createWhenNotExists() {
        // given
        llmMockWebServer.enqueue(new MockResponse().setResponseCode(404)); // 컬렉션 존재 X
        llmMockWebServer.enqueue(new MockResponse().setResponseCode(200)); // 생성 성공

        // when
        Mono<Void> result = qdrantDocumentService.createDataSet(COLLECTION_NAME);

        // then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("createDataSet - 컬렉션이 이미 존재하면 아무것도 하지 않음")
    void createDataSet_doNothingWhenExists() {
        // given
        llmMockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // when
        Mono<Void> result = qdrantDocumentService.createDataSet(COLLECTION_NAME);

        // then
        StepVerifier.create(result)
                .verifyComplete();
        // 생성된 건수 확인
        Assertions.assertEquals(0, llmMockWebServer.getRequestCount());
    }
}
