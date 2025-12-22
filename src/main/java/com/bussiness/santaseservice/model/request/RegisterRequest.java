package com.bussiness.santaseservice.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 5, max = 20, message = "Username must be between 5 and 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Username can contain only letters and digits")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 5, max = 50, message = "Password must be between 5 and 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9!@#$%^&*()_+=\\-.,?]+$", message = "Password can contain only letters, digits, and special symbols")
    private String password;
}
