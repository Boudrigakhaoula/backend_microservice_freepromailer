package com.pfe.authservice.dto.request;
import com.pfe.authservice.enums.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class AdminUpdateUserRequest {
    @NotBlank @Size(min=2,max=100) private String firstName;
    @NotBlank @Size(min=2,max=100) private String lastName;
    @NotBlank @Email               private String email;
    @NotNull                       private UserRole role;
    @NotNull                       private Boolean active;
}

