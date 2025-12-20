package pl.gesieniec.gsmseller;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Slf4j
@EnableJpaAuditing
@SpringBootApplication
public class GsmSellerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GsmSellerApplication.class, args);
    }

    @PostConstruct
    void debugDb() {
        log.info("BUBA DATABASE_URL = {}", System.getenv("DATABASE_URL"));
    }
}
