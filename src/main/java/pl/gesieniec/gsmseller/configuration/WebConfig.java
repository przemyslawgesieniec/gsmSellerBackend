package pl.gesieniec.gsmseller.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import pl.gesieniec.gsmseller.eventlog.EventLogInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final EventLogInterceptor eventLogInterceptor;

    public WebConfig(EventLogInterceptor eventLogInterceptor) {
        this.eventLogInterceptor = eventLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(eventLogInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/v1/admin/event-log/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:63343", "http://127.0.0.1:63343")
            .allowedMethods("GET", "POST", "PUT", "DELETE");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
            .addResourceHandler("/uploads/**")
            .addResourceLocations("file:/app/uploads/")
            .setCachePeriod(3600);
    }
}
