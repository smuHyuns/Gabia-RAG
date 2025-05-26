package gabia.internship.god.integration.generate.service.openai;

import gabia.internship.god.generate.dto.request.openAI.AnswerRequestDto;
import gabia.internship.god.search.dto.request.qdrant.SearchedDocument;



import java.util.List;

public class OpenAIGenerateServiceUtils {
    // Mock AnswerRequestDto
    public static AnswerRequestDto makeMockAnswerRequestDto(String question) {
        SearchedDocument document = SearchedDocument.builder()
                .question("과거 질문 내용")
                .answer("과거 답변 내용")
                .build();

        return AnswerRequestDto.builder()
                .question(question)
                .searchedDocuments(List.of(document))
                .build();
    }

    // Mock SSEBody
    public static String makeMockSSEBody() {
        return """
                data: {"id":"test-id","object":"chat.completion.chunk","created":1712565000,"model":"gpt-4o-mini","service_tier":"standard","system_fingerprint":"fingerprint-xyz","choices":[{"index":0,"delta":{"content":"안녕하세요!"},"logprobs":null,"finish_reason":null}]}
                
                data: [DONE]
                
                """;
    }
}
