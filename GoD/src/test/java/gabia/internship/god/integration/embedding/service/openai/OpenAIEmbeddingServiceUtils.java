package gabia.internship.god.integration.embedding.service.openai;

public class OpenAIEmbeddingServiceUtils {

    public static String makeMockEmbeddingResponseDto(String model) {
        return String.format("""
                {
                  "object": "list",
                  "data": [{
                    "object": "embedding",
                    "index": 0,
                    "embedding": [0.123, 0.456, 0.789]
                  }],
                  "model": "%s",
                  "usage": {
                    "prompt_tokens": 17,
                    "total_tokens": 17
                  }
                }
                """, model);
    }
}
