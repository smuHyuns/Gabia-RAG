package gabia.internship.god.common.config.llm;

import gabia.internship.god.ask.dto.request.EQuestionType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@Component
public class PromptProperties implements InitializingBean {

    @Value("${llm.prompting.path}")
    private Resource promptYaml;

    private String base;
    private Map<String, String> types;

    @Override
    public void afterPropertiesSet() throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream is = promptYaml.getInputStream()) {
            Map<String, Object> data = yaml.load(is);
            this.base = (String) data.get("base");
            this.types = (Map<String, String>) data.get("types");
        }
    }

    public String getPrompt(EQuestionType type) {
        return base + "\n\n" + types.getOrDefault(type.name(), "");
    }
}
