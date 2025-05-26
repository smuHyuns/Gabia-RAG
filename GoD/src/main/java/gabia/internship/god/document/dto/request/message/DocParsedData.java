package gabia.internship.god.document.dto.request.message;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DocParsedData(@JsonProperty("doc_id") int docId,
                            String question, String answer) {
}
