package gabia.internship.god.search.dto.request.qdrant;

import lombok.Builder;

@Builder
public record SearchedDocument(
        String question,
        String answer
) {
}
