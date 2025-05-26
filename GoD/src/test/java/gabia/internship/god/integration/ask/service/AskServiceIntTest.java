package gabia.internship.god.integration.ask.service;


import gabia.internship.god.ask.dto.request.EQuestionType;
import gabia.internship.god.ask.service.AskService;
import gabia.internship.god.generate.dto.response.opneAI.AnswerResponseDto;
import gabia.internship.god.integration.base.QdrantBaseIntegrationTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;

import static gabia.internship.god.integration.ask.service.AskServiceUtil.makeAskMockEmbeddingDto;
import static gabia.internship.god.integration.generate.service.openai.OpenAIGenerateServiceUtils.makeMockSSEBody;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AskServiceIntTest extends QdrantBaseIntegrationTest {
    @Autowired
    private AskService askService;

    private MockWebServer llmMockWebServer;
    private String question;
    private EQuestionType category;

    @Value("${port.llm}")
    private Integer llmPort;
    @Value("${llm.model.embedding}")
    private String embeddingModel;

    @BeforeAll
    void startMockServer() throws IOException {
        llmMockWebServer = new MockWebServer();
        llmMockWebServer.start(llmPort);

        // 사용자 요청 초기 세팅
        question = "테스트용 질문";
        category = EQuestionType.INQUIRY;
    }

    @AfterAll
    void shutdownMockServer() throws IOException {
        llmMockWebServer.shutdown();
    }

    /*
        응답은 총 3개 주어집니다.
        embeddingService.embedding -> embeddingData // enqueue 1회
        searchService.searchDocsForQuestion(embeddingData, questionType) -> answerData // qdrant
        generateService.makeResponse(makeAnswerRequest(question,answerData)) // enqueue 1회
        총 enqueue 2회와 1회의 데이터베이스 조회가 필요한 테스트 입니다.
    */

    @DisplayName("질문 응답 생성 테스트 - 정상 작동")
    @Test
    void answerToQuestion_success() {
        // given
        // 1.embeddingService.embedding
        String embeddingResponse = makeAskMockEmbeddingDto(embeddingModel);
        llmMockWebServer.enqueue(new MockResponse()
                .setBody(embeddingResponse)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        );

        // 2. searchDocsForQuestion은 Qdrant 컨테이너가 실제로 동작하므로 mocking 불필요
        // 테스트용 Qdrant Container에 들어가있는 정보 : Collection명

        // 3. generateService.makeResponse
        String mockSSEBody = makeMockSSEBody();
        llmMockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "text/event-stream")
                .setChunkedBody(mockSSEBody, 10)
        );

        // when
        Flux<AnswerResponseDto> answer = askService.answerToQuestion(question, category);

        // then
        StepVerifier.create(answer.collectList())
                .assertNext(list -> {
                    String fullAnswer = list.stream()
                            .map(dto -> dto.choices().get(0).delta().content())
                            .reduce("", String::concat);

                    assertThat(fullAnswer).isNotBlank();
                })
                .verifyComplete();
    }


    @DisplayName("질문 응답 생성 테스트 - 질문 임베딩 실패")
    @Test
    void answerToQuestion_question_embedding_fail() {
        // given
        llmMockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{ \"error\": \"Internal Server Error\" }")
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE));

        // when
        Flux<AnswerResponseDto> answer = askService.answerToQuestion(question, category);

        // then
        StepVerifier.create(answer)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("OpenAI API 호출 실패"))
                .verify();
    }

    @DisplayName("질문 응답 생성 테스트 - 응답 생성 실패(OpenAI 서버 오류)")
    @Test
    void answerToQuestion_question_make_response_fail() {
        // given
        String embeddingResponse = makeAskMockEmbeddingDto(embeddingModel);
        llmMockWebServer.enqueue(new MockResponse()
                .setBody(embeddingResponse)
                .addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        );

        llmMockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"Internal Server Error\"}"));

        // when
        Flux<AnswerResponseDto> answer = askService.answerToQuestion(question, category);

        // then
        StepVerifier.create(answer)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("응답 생성, OpenAI API 호출 실패")
                )
                .verify();
    }
}