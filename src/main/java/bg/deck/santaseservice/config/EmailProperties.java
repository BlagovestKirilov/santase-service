package bg.deck.santaseservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "mail")
@Configuration
public class EmailProperties {
    public static final String SMTP_AUTH = "mail.smtp.auth";
    public static final String STARTTLS_ENABLE = "mail.smtp.starttls.enable";
    private String host;
    private Integer port;
    private String username;
    private String password;
}
