package gabia.internship.god.unit.ask.controller;

import gabia.internship.god.ask.controller.AskController;
import gabia.internship.god.ask.dto.request.EQuestionType;
import gabia.internship.god.ask.service.AskService;
import gabia.internship.god.generate.dto.response.opneAI.AnswerResponseDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;

@WebFluxTest(controllers = AskController.class)
public class AskControllerUnitTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AskService askService;

    private AnswerResponseDto mockResponse;
    private AnswerResponseDto mockStopResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new AnswerResponseDto(
                "chatcmpl-123",
                "chat.completion.chunk",
                1111111111L,
                "gpt-4",
                "standard",
                "fingerprint-123",
                List.of(new AnswerResponseDto.Choice(
                        0L,
                        new AnswerResponseDto.Delta("안녕하세요"),
                        null,
                        null
                ))
        );

        mockStopResponse = new AnswerResponseDto(
                "chatcmpl-124",
                "chat.completion.chunk",
                1111111112L,
                "gpt-4",
                "standard",
                "fingerprint-124",
                List.of(new AnswerResponseDto.Choice(
                        1L,
                        new AnswerResponseDto.Delta(null),
                        null,
                        "stop"
                ))
        );
    }


    @Test
    @DisplayName("응답 요청 성공 - 정상 응답 테스트")
    void answerToQuestion_success() {
        // given
        Mockito.when(askService.answerToQuestion(eq("테스트 질문"), eq(EQuestionType.INQUIRY)))
                .thenReturn(Flux.just(mockResponse, mockStopResponse).delayElements(Duration.ofMillis(10)));

        // when & then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/ask")
                        .queryParam("question", "테스트 질문")
                        .queryParam("questionType", "INQUIRY")
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(AnswerResponseDto.class)
                .hasSize(2)
                .consumeWith(result -> {
                    List<AnswerResponseDto> body = result.getResponseBody();
                    assert body != null;
                    assert body.get(0).choices().get(0).delta().content().equals("안녕하세요");
                    assert body.get(1).choices().get(0).finishReason().equals("stop");
                });
    }

    @Test
    @DisplayName("응답 요청 실패 - 유효하지 않은 questionType")
    void answerToQuestion_invalidQuestionType() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/ask")
                        .queryParam("question", "테스트 질문")
                        .queryParam("questionType", "INVALID_TYPE")
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("응답 요청 실패 - question 파라미터 누락")
    void answerToQuestion_missingQuestionParam() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/ask")
                        .queryParam("questionType", "INQUIRY")
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("응답 요청 실패 - 서비스 내부 오류")
    void answerToQuestion_serviceError() {
        Mockito.when(askService.answerToQuestion(eq("테스트 질문"), eq(EQuestionType.INQUIRY)))
                .thenReturn(Flux.error(new RuntimeException("Internal Error")));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/ask")
                        .queryParam("question", "테스트 질문")
                        .queryParam("questionType", "INQUIRY")
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().is5xxServerError();
    }

}
