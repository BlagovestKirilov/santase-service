package bg.deck.service;

import bg.deck.config.TemplateLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    /** The production mail thread: one, so emails leave in order. */
    @Bean(destroyMethod = "shutdown")
    ExecutorService mailExecutor() {
        return Executors.newSingleThreadExecutor(Thread.ofVirtual().name("mail-", 0).factory());
    }

    @Bean
    PlatformTransactionManager transactionManager() {
        return new NoDatabaseTransactionManager();
    }
}
