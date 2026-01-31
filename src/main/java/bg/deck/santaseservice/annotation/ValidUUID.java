package bg.deck.santaseservice.annotation;


import bg.deck.santaseservice.constant.ValidationConstants;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.springframework.validation.annotation.Validated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidUUIDValidator.class)
@Validated
public @interface ValidUUID {
    String message() default ValidationConstants.INVALID_TOKEN;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

