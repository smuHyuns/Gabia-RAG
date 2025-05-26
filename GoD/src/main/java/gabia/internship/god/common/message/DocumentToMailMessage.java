package gabia.internship.god.common.message;

import lombok.Builder;

@Builder
public record DocumentToMailMessage(
        String uploadId,
        String collectionName,
        String email,
        int total,
        int parsing,
        int upload,
        int embeddingFail,
        int parsingFail,
        int uploadFail,
        boolean isSuccess
) {
}
