package com.pfe.authservice.dto.response;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email, firstName, lastName, role;
    private Boolean active;
    private LocalDateTime createdAt, updatedAt;
}

