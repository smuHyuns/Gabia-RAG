package gabia.internship.god.integration.embedding.service.openai;

import gabia.internship.god.embedding.service.OpenAIEmbeddingService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.io.IOException;

import static gabia.internship.god.integration.embedding.service.openai.OpenAIEmbeddingServiceUtils.makeMockEmbeddingResponseDto;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OpenAIEmbeddingServiceIntTest {
    private MockWebServer llmMockWebServer;

    @Autowired
    private OpenAIEmbeddingService openAIEmbeddingService;

    @Value("${port.llm}")
    private Integer llmPort;

    @Value("${llm.model.embedding}")
    private String embeddingModel;

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
    @DisplayName("임베딩 테스트 - 정상 작동 테스트")
    void embedding_success() {
        // given
        String question = "테스트란 무엇인가요?";
        String jsonResponse = makeMockEmbeddingResponseDto(embeddingModel);
        llmMockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));

        // when & then
        StepVerifier.create(openAIEmbeddingService.embedding(question))
                .assertNext(response -> {
                    assertThat(response.model()).isEqualTo(embeddingModel);
                    assertThat(response.data()).isNotNull();
                    assertThat(response.data().get(0).embedding())
                            .containsExactly(0.123, 0.456, 0.789);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("임베딩 테스트 - OpennAI 서버 오류")
    void embedding_failed_by_500() {
        // given
        String question = "에러 테스트용 질문";

        llmMockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{ \"error\": \"Internal Server Error\" }")
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));

        // when & then
        StepVerifier.create(openAIEmbeddingService.embedding(question))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("OpenAI API 호출 실패"))
                .verify();
    }
}
