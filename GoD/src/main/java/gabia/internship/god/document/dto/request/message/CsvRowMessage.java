package gabia.internship.god.document.dto.request.message;

import java.util.Map;

public record CsvRowMessage(
        int docId,
        Map<String, String> payload
) {
}
