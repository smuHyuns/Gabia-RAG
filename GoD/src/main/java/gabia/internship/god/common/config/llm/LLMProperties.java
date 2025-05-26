package gabia.internship.god.common.config.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm")
public record LLMProperties(
        Model model,

        String prompting
) {
    public record Model(
            String embedding,

            String generate
    ) {

    }
}
