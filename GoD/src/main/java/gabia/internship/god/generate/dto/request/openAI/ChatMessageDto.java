package gabia.internship.god.generate.dto.request.openAI;

import gabia.internship.god.common.constants.Constants;
import lombok.Builder;

@Builder
public record ChatMessageDto(
        String role,

        String content
) {

    public static ChatMessageDto makeMessageForResponse(String prompt) {
        return ChatMessageDto.builder()
                .role(Constants.PROMPTING_SYSTEM)
                .content(prompt)
                .build();
    }

    public static ChatMessageDto makeUserMessage(String userRequest) {
        return ChatMessageDto.builder()
                .role(Constants.PROMPTING_USER)
                .content(userRequest)
                .build();
    }

}
