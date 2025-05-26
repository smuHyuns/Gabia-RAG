package gabia.internship.god.embedding.dto.request.openAI;


import java.util.List;

public record EmbeddingRequestDto(
        String model,

        List<String> input
) {

    public static EmbeddingRequestDto makeEmbeddingRequest(String model, List<String> inputs) {
        return new EmbeddingRequestDto(model, inputs);
    }
}
