package gabia.internship.god.integration.ask.service;

public class AskServiceUtil {
    public static String makeAskMockEmbeddingDto(String model) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1536; i++) {
            sb.append("0.01");
            if (i < 1535) {
                sb.append(", ");
            }
        }

        return String.format("""
                {
                  "object": "list",
                  "data": [{
                    "object": "embedding",
                    "index": 0,
                    "embedding": [%s]
                  }],
                  "model": "%s",
                  "usage": {
                    "prompt_tokens": 17,
                    "total_tokens": 17
                  }
                }
                """, sb, model);
    }

}
