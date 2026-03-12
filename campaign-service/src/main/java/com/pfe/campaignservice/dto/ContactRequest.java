package com.pfe.campaignservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email doit être valide")
    private String email;

    private String firstName;
    private String lastName;
    private String company;
    private String phone;

    @NotNull(message = "L'ID de la liste de contacts est obligatoire")
    private Long contactListId;
}
