package gabia.internship.god.integration.search.service.qdrant;

import gabia.internship.god.document.dto.request.qdrant.UploadPointsDto;
import gabia.internship.god.document.dto.request.vector.VectorDataDto;
import gabia.internship.god.embedding.dto.response.openAI.EmbeddingResponseDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class QdrantSearchServiceUtils {

    // mock embedding 생성
    public static EmbeddingResponseDto makeMockEmbeddingResponse() {
        List<Double> vector = makeVectorList();

        List<EmbeddingResponseDto.EmbeddingDataDto> data = List.of(
                new EmbeddingResponseDto.EmbeddingDataDto(
                        "embedding",
                        0,
                        vector
                )
        );
        EmbeddingResponseDto.EmbeddingUsageDto usage = new EmbeddingResponseDto.EmbeddingUsageDto(
                15,
                15
        );
        return new EmbeddingResponseDto("list", data, "text-embedding-3-small", usage);
    }

    // mock embedding(invalid) 생성
    public static EmbeddingResponseDto makeInvalidMockEmbeddingResponse() {
        // 의도적으로 벡터 사이즈를 잘못 생성 (ex. 10차원)
        List<Double> invalidVector = DoubleStream.generate(() -> 0.1234)
                .limit(10) // ← 벡터 사이즈 오류 유도 (정상은 1536)
                .boxed()
                .collect(Collectors.toList());
        List<EmbeddingResponseDto.EmbeddingDataDto> data = List.of(
                new EmbeddingResponseDto.EmbeddingDataDto(
                        "embedding",
                        0,
                        invalidVector
                )
        );
        EmbeddingResponseDto.EmbeddingUsageDto usage = new EmbeddingResponseDto.EmbeddingUsageDto(
                15,
                15
        );
        return new EmbeddingResponseDto("list", data, "text-embedding-3-small", usage);
    }


    // 업로드용 point 생성
    public static UploadPointsDto makeTestPoint() {
        Map<String, Object> payload = Map.of(
                "subject", "테스트 제목",
                "content", "테스트 내용입니다."
        );
        VectorDataDto vectorData = VectorDataDto.of(
                1,
                makeVectorList(),
                payload
        );
        return UploadPointsDto.makeUploadPointsDto(List.of(vectorData));
    }


    // 1536차원 vectorlist 생성
    public static List<Double> makeVectorList() {
        return IntStream.range(0, 1536)
                .mapToObj(i -> Math.random())
                .collect(Collectors.toList());
    }

}



