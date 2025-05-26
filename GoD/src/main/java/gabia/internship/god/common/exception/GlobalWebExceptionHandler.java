package gabia.internship.god.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(-2)
// Order(-2)의 이유
// Spring WebFlux에서 예외를 전역적으로 처리하기 위해 설정
// Spring Boot의 기본 전역 예외 처리기(DefaultErrorWebExceptionHandler)와 동일한 우선순위로 설정함
// @Order를 명시하지 않으면 우선순위가 낮아져, 등록은 되어도 실제로 호출되지 않을 수 있음
public class GlobalWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 기본 예외
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "서버 내부 오류가 발생했습니다.";
        String cause = null;

        // 커스텀 예외 처리
        if (ex instanceof CommonException commonEx) {
            status = commonEx.getStatus();
            message = commonEx.getMessage();
            cause = (commonEx.getCause() != null)
                    ? commonEx.getCause().getMessage()
                    : null;
        }

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", status.value());
        responseBody.put("message", message);
        responseBody.put("cause", cause);
        responseBody.put("error", ex.getClass().getSimpleName());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(responseBody);
        } catch (Exception e) {
            bytes = "{\"message\":\"응답 변환 실패\"}".getBytes(StandardCharsets.UTF_8);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
