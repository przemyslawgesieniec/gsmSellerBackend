package pl.gesieniec.gsmseller.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class ExecutorConfig {

    @Bean
    public ExecutorService ocrExecutor() {
        return Executors.newFixedThreadPool(3);
    }


}
