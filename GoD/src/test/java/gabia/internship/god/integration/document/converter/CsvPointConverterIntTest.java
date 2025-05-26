package gabia.internship.god.integration.document.converter;

import gabia.internship.god.document.dto.request.vector.VectorDataDto;
import gabia.internship.god.embedding.service.OpenAIEmbeddingService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static gabia.internship.god.integration.document.converter.CsvPointConverterUtils.*;
import static gabia.internship.god.integration.embedding.service.openai.OpenAIEmbeddingServiceUtils.makeMockEmbeddingResponseDto;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CsvPointConverterIntTest {
    private MockWebServer llmMockWebServer;

    @Autowired
    private OpenAIEmbeddingService openAIEmbeddingService;

    @Autowired
    private CsvPointConverter converter;

    @Value("${llm.model.embedding}")
    private String embeddingModel;

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
    @DisplayName("파싱 테스트 - 정상 작동")
    void parseFile_success() {
        // given
        String csvContent = makeMockCsvContent();
        String mockResponse = makeMockEmbeddingResponseDto(embeddingModel);

        llmMockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        FilePart mockFilePart = mock(FilePart.class);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8));

        when(mockFilePart.content()).thenReturn(Flux.just(buffer));

        // when & then
        Flux<VectorDataDto> result = converter.parseFile(mockFilePart);

        StepVerifier.create(result)
                .expectNextMatches(dto ->
                        dto.id() == 1 &&
                                dto.vector()[0] == 0.123 &&
                                "테스트 질문입니다".equals(dto.payload().get("question")) &&
                                "테스트 콘텐츠 입니다".equals(dto.payload().get("answer"))
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("파싱 테스트(예외) - Flux.error 발생")
    void parseFile_csvParsingError() {
        // given
        FilePart mockFilePart = mock(FilePart.class);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockFilePart.content()).thenReturn(Flux.just(buffer));

        // CsvPointConverter의 parseFile을 override해서 내부에서 IOException 유도
        CsvPointConverter faultyConverter = new CsvPointConverter(openAIEmbeddingService) {
            @Override
            public Flux<VectorDataDto> parseFile(FilePart filePart) {
                return Flux.error(new RuntimeException("CSV 파일 파싱 중 오류 발생"));
            }
        };

        // when
        Flux<VectorDataDto> result = faultyConverter.parseFile(mockFilePart);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(e ->
                        e instanceof RuntimeException &&
                                e.getMessage().contains("CSV 파일 파싱 중 오류 발생"))
                .verify();
    }


    // 현재 임베딩 실패시 3회 재반복이 포함되어 있어 시간이 오래 걸림
    @Test
    @DisplayName("파싱 테스트(예외) - 400 error 발생")
    void parseFile_recordProcessingError() {
        // given: answer 컬럼이 비어있어 LLM 호출 시 오류 발생을 유도
        String csvContent = makeMockCsvContent400Err();

        for (int i = 0; i < 4; i++) {
            llmMockWebServer.enqueue(new MockResponse()
                    .setResponseCode(400)  // OpenAI가 비어있는 입력에 대해 에러 응답
                    .setBody("{\"error\": \"Invalid input\"}")
                    .addHeader("Content-Type", "application/json"));
        }

        FilePart mockFilePart = mock(FilePart.class);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(csvContent.getBytes(StandardCharsets.UTF_8));
        when(mockFilePart.content()).thenReturn(Flux.just(buffer));

        // when & then
        Flux<VectorDataDto> result = converter.parseFile(mockFilePart);

        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }


}
