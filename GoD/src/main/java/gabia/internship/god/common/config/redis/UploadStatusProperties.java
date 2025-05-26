package gabia.internship.god.common.config.redis;

import org.springframework.stereotype.Component;

@Component
public record UploadStatusProperties() {
    public String total(String uploadId) {
        return uploadId + ":total-count";
    }

    public String parsing(String uploadId) {
        return uploadId + ":parsing-count";
    }

    public String upload(String uploadId) {
        return uploadId + ":upload-count";
    }

    public String embeddingFail(String uploadId) {
        return uploadId + ":embedding-fail";
    }

    public String documentFail(String uploadId) {
        return uploadId + ":document-fail";
    }

    public String pendingFail(String uploadId) {
        return uploadId + ":pending-fail";
    }

    public String collection(String uploadId) {
        return uploadId + ":collection";
    }

    public String email(String uploadId) {
        return uploadId + ":email";
    }
}
