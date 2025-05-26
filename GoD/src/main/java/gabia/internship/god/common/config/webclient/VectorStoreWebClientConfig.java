package gabia.internship.god.common.config.webclient;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class VectorStoreWebClientConfig {

    @Value("${base-url.vectorstore}")
    private String vectorStoreUrl;

    @Bean
    public WebClient vectorStoreWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(120)) // 응답 대기 시간 (서버가 응답해야 하는 시간)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000); // 연결 타임아웃 (서버 연결 시도 시간)

        return WebClient.builder()
                .baseUrl(vectorStoreUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
