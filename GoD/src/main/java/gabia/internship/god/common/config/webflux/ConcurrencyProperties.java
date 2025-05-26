package gabia.internship.god.common.config.webflux;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.webflux-concurrency")
public record ConcurrencyProperties(
        int document,
        int documentMail,
        int parsing,
        int embedding) {
}
