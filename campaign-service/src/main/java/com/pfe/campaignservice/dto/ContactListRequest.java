package com.pfe.campaignservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactListRequest {

    @NotBlank(message = "Le nom de la liste est obligatoire")
    private String name;

    private String description;
}
