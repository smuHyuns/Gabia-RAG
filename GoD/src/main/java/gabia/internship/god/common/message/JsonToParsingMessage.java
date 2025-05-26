package gabia.internship.god.common.message;

import gabia.internship.god.document.dto.request.message.DocParsedData;

import java.util.List;

public record JsonToParsingMessage(
        String uploadId,
        String dataSet,
        String email,
        List<DocParsedData> data
) {
}
