package pl.gesieniec.gsmseller.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

    @Bean
    WebClient openAiWebClient(OpenAiProperties properties) {
        return WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader(
                "Authorization",
                "Bearer " + properties.getApiKey()
            )
            .build();
    }
}
