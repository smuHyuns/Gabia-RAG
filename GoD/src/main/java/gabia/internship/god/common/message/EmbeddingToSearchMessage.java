package gabia.internship.god.common.message;

import gabia.internship.god.ask.dto.request.EQuestionType;

import java.util.List;

public record EmbeddingToSearchMessage(

        String userId,

        EQuestionType type,

        String question,

        List<Double> values
) {
}
