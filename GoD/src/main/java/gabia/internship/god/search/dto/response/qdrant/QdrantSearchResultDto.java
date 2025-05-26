package gabia.internship.god.search.dto.response.qdrant;

import java.util.List;
import java.util.Map;

public record QdrantSearchResultDto(
        List<Result> result
) {
    public record Result(
            Object id,
            float score,
            Map<String, Object> payload
    ) {
    }
}
