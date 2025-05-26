package gabia.internship.god.common.message;

import gabia.internship.god.document.dto.request.vector.VectorDataDto;

import java.util.List;

public record EmbeddingToUploadMessage(
        String collectionName,
        List<VectorDataDto>vectors,
        String uploadId,
        Boolean isLast
) {
}