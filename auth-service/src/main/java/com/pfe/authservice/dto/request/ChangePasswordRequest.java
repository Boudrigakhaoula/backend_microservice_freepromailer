package com.pfe.authservice.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ChangePasswordRequest {
    @NotBlank                      private String currentPassword;
    @NotBlank @Size(min=6)         private String newPassword;
    @NotBlank                      private String confirmPassword;
}

