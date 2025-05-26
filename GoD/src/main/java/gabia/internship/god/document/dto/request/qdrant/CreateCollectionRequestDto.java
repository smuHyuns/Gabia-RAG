package gabia.internship.god.document.dto.request.qdrant;

import com.fasterxml.jackson.annotation.JsonProperty;
import gabia.internship.god.common.constants.Constants;
import gabia.internship.god.document.dto.request.vector.VectorInfoDto;

public record CreateCollectionRequestDto(
        VectorInfoDto vectors,

        @JsonProperty("on_disk_payload")
        boolean onDiskPayload,

        @JsonProperty("hnsw_config")
        HnswConfigDto hnswConfigDto


) {

    public record HnswConfigDto(
            @JsonProperty("on_disk")
            boolean onDisk
    ) {

    }
    public static CreateCollectionRequestDto makeCreateCollectionRequestDto() {
        return new CreateCollectionRequestDto(
                new VectorInfoDto(Constants.SIZE, Constants.DISTANCE),
                true,
                new HnswConfigDto(true)
        );
    }
}
