package gabia.internship.god.integration.generate.service.openai;


import gabia.internship.god.generate.dto.request.openAI.AnswerRequestDto;
import gabia.internship.god.generate.dto.response.opneAI.AnswerResponseDto;
import gabia.internship.god.generate.service.OpenAIGenerateService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;

import static gabia.internship.god.integration.generate.service.openai.OpenAIGenerateServiceUtils.makeMockAnswerRequestDto;
import static gabia.internship.god.integration.generate.service.openai.OpenAIGenerateServiceUtils.makeMockSSEBody;


@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OpenAIGenerateServiceIntTest {
    private MockWebServer llmMockWebServer;

    @Autowired
    private OpenAIGenerateService openAIGenerateService;

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
    @DisplayName("응답 생성 테스트 - 정상 작동")
    void makeResponse() {
        // Given
        String mockSSEBody = makeMockSSEBody();

        llmMockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "text/event-stream")
                .setChunkedBody(mockSSEBody, 10));

        AnswerRequestDto request = makeMockAnswerRequestDto("테스트용 질문이 있습니다.");

        // When
        Flux<AnswerResponseDto> response = openAIGenerateService.makeResponse(request);

        // Then
        StepVerifier.create(response)
                .expectNextMatches(res ->
                        res.choices() != null &&
                                !res.choices().isEmpty() &&
                                "안녕하세요!".equals(res.choices().get(0).delta().content())
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("응답 생성 테스트 - OPEN AI 응답 X")
    void makeResponse_OpenAIServerError() {
        // Given
        llmMockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"Internal Server Error\"}"));

        AnswerRequestDto request = makeMockAnswerRequestDto("OpenAI 오류 시나리오 테스트");

        // When
        Flux<AnswerResponseDto> response = openAIGenerateService.makeResponse(request);

        // Then
        StepVerifier.create(response)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("응답 생성, OpenAI API 호출 실패")
                )
                .verify();
    }

    @Test
    @DisplayName("응답 생성 테스트 - 4xx,5xx 에러 처리")
    void makeResponse_RuntimeException() {
        // Given - WebClientResponseException 발생 유도 (예: 400 Bad Request)
        llmMockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\": \"Bad Request\"}")
                .addHeader("Content-Type", "application/json"));

        AnswerRequestDto request = makeMockAnswerRequestDto("WebClientResponseException RuntimeException발생 테스트");

        // When
        Flux<AnswerResponseDto> response = openAIGenerateService.makeResponse(request);

        // Then
        StepVerifier.create(response)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("응답 생성 테스트 - 마지막 DONE 처리")
    void makeResponse_DONE() {
        // Given
        String doneOnlySSE = """
                data: [DONE]
                """;

        llmMockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "text/event-stream")
                .setChunkedBody(doneOnlySSE, 5));

        AnswerRequestDto request = makeMockAnswerRequestDto("DONE 메시지만 포함된 테스트");

        // When
        Flux<AnswerResponseDto> response = openAIGenerateService.makeResponse(request);

        // Then
        StepVerifier.create(response)
                .verifyComplete();
    }
}
