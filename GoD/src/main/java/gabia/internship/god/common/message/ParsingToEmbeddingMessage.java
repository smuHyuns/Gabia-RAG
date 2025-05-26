package gabia.internship.god.common.message;

import gabia.internship.god.document.dto.request.message.CsvRowMessage;

import java.util.List;

public record ParsingToEmbeddingMessage(
        String uploadId,
        String collectionName,
        List<CsvRowMessage> rows
) {}

