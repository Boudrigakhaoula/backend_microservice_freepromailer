package com.pfe.campaignservice.dto;


import com.pfe.campaignservice.enums.TemplateCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequest {

    @NotBlank(message = "Le nom du template est obligatoire")
    private String name;

    private String subject;
    private String htmlContent;
    private String textContent;
    private TemplateCategory category;
    private String description;
    private String previewText;
}
