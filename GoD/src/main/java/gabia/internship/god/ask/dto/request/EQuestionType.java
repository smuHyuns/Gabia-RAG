package gabia.internship.god.ask.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum EQuestionType {
    INQUIRY("INQUIRY"),

    VOC("VOC");

    private final String type;

    @JsonCreator
    public static EQuestionType from(String input) {
        return Arrays.stream(EQuestionType.values())
                .filter(e -> e.type.equalsIgnoreCase(input))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 질문 유형입니다: " + input));
    }
}
