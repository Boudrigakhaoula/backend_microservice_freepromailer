package com.pfe.authservice.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class RegisterRequest {
    @NotBlank @Email        private String email;
    @NotBlank @Size(min=6)  private String password;
    @NotBlank               private String firstName;
    @NotBlank               private String lastName;
}

