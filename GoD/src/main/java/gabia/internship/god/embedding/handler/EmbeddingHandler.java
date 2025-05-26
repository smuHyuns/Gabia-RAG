package gabia.internship.god.embedding.handler;


import gabia.internship.god.common.handler.MessageHandler;
import gabia.internship.god.common.message.QuestionToEmbeddingMessage;
import gabia.internship.god.core.llm.LLMEmbeddingService;
import gabia.internship.god.embedding.dto.response.openAI.EmbeddingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class EmbeddingHandler implements MessageHandler<QuestionToEmbeddingMessage> {
    private final LLMEmbeddingService<EmbeddingResponseDto, String, QuestionToEmbeddingMessage> openAIEmbeddingService;

    @Override
    public Mono<Void> handle(QuestionToEmbeddingMessage message, Integer retryCount) {
        return openAIEmbeddingService.embeddingQuestion(message);
    }
}
