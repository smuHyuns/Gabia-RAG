package gabia.internship.god.common.message;

import gabia.internship.god.ask.dto.request.EQuestionType;

public record QuestionToEmbeddingMessage(
        String userId,

        String question,

        EQuestionType type
) {
}
