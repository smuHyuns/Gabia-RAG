package gabia.internship.god.unit.search.service.qdrant;

import gabia.internship.god.embedding.dto.response.openAI.EmbeddingResponseDto;
import gabia.internship.god.search.dto.response.qdrant.QdrantSearchResultDto;
import gabia.internship.god.search.service.QdrantSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QdrantSearchServiceUnitTest {

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private QdrantSearchService qdrantSearchService;

    private EmbeddingResponseDto embeddingResponseDto;
    private String collectionName = "test";
    private QdrantSearchResultDto expectedResponse;

    @BeforeEach
    void setUp() {
        // 공통 테스트 데이터 세팅 - EmbeddingResponseDto
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        EmbeddingResponseDto.EmbeddingDataDto embeddingDataDto =
                new EmbeddingResponseDto.EmbeddingDataDto("embedding", 0, embedding);
        EmbeddingResponseDto.EmbeddingUsageDto usageDto =
                new EmbeddingResponseDto.EmbeddingUsageDto(10, 15);

        embeddingResponseDto =
                new EmbeddingResponseDto("list", List.of(embeddingDataDto), "test-model", usageDto);

        // 공통 테스트 데이터 세팅 - QdrantSearchResultDto
        Map<String, Object> payload = Map.of("title", "Sample Document", "category", "test");
        QdrantSearchResultDto.Result result =
                new QdrantSearchResultDto.Result("doc-1", 0.98f, payload);
        expectedResponse = new QdrantSearchResultDto(List.of(result));

        // Webclient 체이닝
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }


    @DisplayName("Qdrant 유사도 문서 탐색 성공 테스트")
    @Test
    void searchDocsForQuestionTest_Sucess() {
        // given
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(QdrantSearchResultDto.class)).thenReturn(Mono.just(expectedResponse));

        // when
        Mono<QdrantSearchResultDto> result = qdrantSearchService.searchDocsForQuestion(embeddingResponseDto, collectionName);

        // then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();
    }


    @DisplayName("Qdrant 유사도 문서 탐색 실패 테스트")
    @Test
    void searchDocsForQuestionTest_Error() {
        // given
        // onStatus는 체이닝을 위해 responseSpec 그대로 리턴
        when(responseSpec.onStatus(any(), any()))
                .thenAnswer(invocation -> {
                    Predicate<HttpStatusCode> statusPredicate = invocation.getArgument(0);
                    Function<ClientResponse, Mono<? extends Throwable>> errorFunction = invocation.getArgument(1);
                    if (statusPredicate.test(HttpStatus.INTERNAL_SERVER_ERROR)) {
                        return invocation.getMock(); // 체이닝 위해 그대로 리턴
                    }
                    return null;
                });
        when(responseSpec.bodyToMono(QdrantSearchResultDto.class))
                .thenReturn(Mono.error(new RuntimeException("Qdrant API 호출 실패")));

        // when
        Mono<QdrantSearchResultDto> result = qdrantSearchService.searchDocsForQuestion(embeddingResponseDto, collectionName);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Qdrant API 호출 실패"))
                .verify();
    }
}
