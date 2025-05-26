package gabia.internship.god.common.message;

import gabia.internship.god.ask.dto.request.EQuestionType;
import gabia.internship.god.search.dto.response.qdrant.QdrantSearchResultDto;

import java.util.List;

public record SearchToGenerateMessage(
        String userId,

        String question,

        EQuestionType type,

        List<QdrantSearchResultDto.Result> searchedDocs
) {
}
