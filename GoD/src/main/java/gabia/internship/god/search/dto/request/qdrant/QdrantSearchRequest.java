package gabia.internship.god.search.dto.request.qdrant;

import gabia.internship.god.common.constants.Constants;

import java.util.List;

public record QdrantSearchRequest(
        List<Double> vector,
        int top,
        boolean with_payload
) {
    public static QdrantSearchRequest makeQdrantSearchRequest(List<Double> vector) {
        return new QdrantSearchRequest(vector, Constants.K, true);
    }
}
