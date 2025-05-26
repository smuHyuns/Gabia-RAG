package gabia.internship.god.common.config.webflux;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.webflux-buffer")
public record BufferProperties(
        int embeddingDocs,
        int qdrantDocs,
        int docsUpload
) {
}
