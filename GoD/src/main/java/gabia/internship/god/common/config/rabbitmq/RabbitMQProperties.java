package gabia.internship.god.common.config.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq")
public record RabbitMQProperties(
        Connection connection,
        Exchange exchange,
        Queue queue,
        Routing routing
) {
    public record Connection(
            String host,
            String username,
            String password
    ) {
    }

    public record Exchange(
            String main,
            String dead
    ) {
    }

    public record Queue(
            String workEmbedding,
            String workSearch,
            String workGenerate,
            String workResponse,
            String workDocument,
            String workDocumentMail,
            String workDocumentParsing,
            String workDocumentCsv,
            String workEmbeddingCsv,
            String dead
    ) {}

    public record Routing(
            String workEmbedding,
            String workSearch,
            String workGenerate,
            String workResponse,
            String workDocument,
            String workDocumentMail,
            String workDocumentParsing,
            String workDocumentCsv,
            String workEmbeddingCsv,
            String dead
    ) {}
}
