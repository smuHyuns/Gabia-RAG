package gabia.internship.god.document.dto.request.qdrant;

import gabia.internship.god.document.dto.request.vector.VectorDataDto;

import java.util.List;

public record UploadPointsDto(
        List<VectorDataDto> points
) {
    public static UploadPointsDto makeUploadPointsDto(List<VectorDataDto> points) {
        return new UploadPointsDto(points);
    }
}
