package gabia.internship.god.generate.dto.request.openAI;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatRequestDto(
        String model,

        List<ChatMessageDto> messages,

        Double temperature,

        Boolean stream,

        Integer n

) {

    public static ChatRequestDto makeRequestForResponseToLLM(String model, String prompt, String questionAndDocs) {
        return ChatRequestDto.builder()
                .model(model)
                .messages(
                        List.of(
                                ChatMessageDto.makeMessageForResponse(prompt),
                                ChatMessageDto.makeUserMessage(questionAndDocs)
                        )
                )
                .temperature(0.2)
                .stream(Boolean.TRUE)
                .n(1)
                .build();
    }
}
