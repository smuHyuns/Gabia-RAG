package gabia.internship.god.common.config.webflux;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // spring webflux로 요청을 보내게 되면,
        // 정적 리소스 매핑이 우선적으로 처리가 되어, 올바른 api path로 전달함에도 불구하고 404 Not Found 에러가 발생함
        // 이를 방지하기 위해 ResourceHandler에 대한 처리를 추가하여 해결
        // 로컬에서는 발생하지 않았지만, 배포 환경에서는 위와 같은 문제가 발생
    }
}
