package gabia.internship.god.embedding.dto.response.openAI;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbeddingResponseDto(
        String object,

        List<EmbeddingDataDto> data,

        String model,

        EmbeddingUsageDto usage
) {
    public record EmbeddingDataDto(
            String Object,

            Integer index,

            List<Double> embedding
    ) {
    }

    public record EmbeddingUsageDto(
            @JsonProperty("prompt_tokens")
            Integer promptTokens,

            @JsonProperty("total_tokens")
            Integer totalTokens
    ) {
    }

}
