package gabia.internship.god.document.dto.request.parsing;

import gabia.internship.god.document.dto.request.message.DocParsedData;

import java.util.List;

public record ParsingResult(
        String dataSet,
        String email,
        List<DocParsedData> data,
        boolean isFailed
) {
}
