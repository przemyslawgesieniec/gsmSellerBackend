package pl.gesieniec.gsmseller.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:63343", "http://127.0.0.1:63343") // dopasuj do swojego frontu
            .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}

