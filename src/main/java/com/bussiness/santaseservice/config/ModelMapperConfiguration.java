package com.bussiness.santaseservice.config;

import com.bussiness.santaseservice.model.User;
import com.bussiness.santaseservice.model.request.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@Configuration
public class ModelMapperConfiguration {

    private final PasswordEncoder passwordEncoder;

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        Converter<String, String> passwordConverter =
                ctx -> ctx.getSource() == null ? null : passwordEncoder.encode(ctx.getSource());

        modelMapper.typeMap(RegisterRequest.class, User.class)
                .addMappings(m -> m.using(passwordConverter)
                        .map(RegisterRequest::getPassword, User::setPassword));

        return modelMapper;
    }
}
