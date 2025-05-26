package gabia.internship.god.generate.dto.response.opneAI;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AnswerResponseDto(
        String id,

        String object,

        Long created,

        String model,

        @JsonProperty("service_tier")
        String serviceTier,

        @JsonProperty("system_fingerprint")
        String systemFingerprint,

        List<Choice> choices


) {
    public record Choice(
            Long index,

            Delta delta,

            String logprobs,

            @JsonProperty("finish_reason")
            String finishReason
    ) {
    }

    public record Delta(
            String content
    ) {
    }
}
