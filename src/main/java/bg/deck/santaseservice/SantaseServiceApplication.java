package bg.deck.santaseservice;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SpringBootApplication
public class SantaseServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(SantaseServiceApplication.class, args);
    }

}
