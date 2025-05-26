package gabia.internship.god.generate.dto.request.openAI;

import gabia.internship.god.search.dto.request.qdrant.SearchedDocument;
import gabia.internship.god.search.dto.response.qdrant.QdrantSearchResultDto;
import lombok.Builder;

import java.util.List;

import static gabia.internship.god.common.constants.Constants.THRESHOLD;

@Builder
public record AnswerRequestDto(
        String question,

        List<SearchedDocument> searchedDocuments
) {
    public static AnswerRequestDto makeAnswerRequest(String question, QdrantSearchResultDto searchResponseDto) {
        return AnswerRequestDto.builder()
                .question(question)
                .searchedDocuments(toSearchedDocuments(searchResponseDto))
                .build();
    }

    private static List<SearchedDocument> toSearchedDocuments(QdrantSearchResultDto response) {
        return response.result().stream()
                .filter(data -> data.score() >= THRESHOLD)
                .map(data -> SearchedDocument.builder()
                        .question(data.payload().get("question").toString())
                        .answer(data.payload().get("answer").toString())
                        .build()
                ).toList();
    }
}
