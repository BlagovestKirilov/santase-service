package bg.deck.santaseservice.service;

import bg.deck.santaseservice.config.TemplateLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.mockito.Mockito.mock;

/**
 * The production event, async and after-commit machinery around
 * {@link EmailService}, with the mail server and the database stood in for.
 */
@Configuration
@EnableAsync
@EnableTransactionManagement
@Import({EmailService.class, TemplateLoader.class})
class EmailTestConfig {

    @Bean
    JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }

    @Bean
    PlatformTransactionManager transactionManager() {
        return new NoDatabaseTransactionManager();
    }
}
