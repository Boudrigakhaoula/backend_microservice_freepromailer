package com.pfe.authservice.dto.response;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AuthResponse {
    private String token;
    @Builder.Default private String type = "Bearer";
    private Long   id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
}

