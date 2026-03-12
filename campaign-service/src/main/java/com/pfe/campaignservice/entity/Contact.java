package com.pfe.campaignservice.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pfe.campaignservice.enums.ContactStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contacts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_contact_email_list",
                columnNames = {"email", "contact_list_id"}),
        indexes = {
                @Index(name = "idx_contact_email", columnList = "email"),
                @Index(name = "idx_contact_status", columnList = "status"),
                @Index(name = "idx_contact_list", columnList = "contact_list_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String firstName;
    private String lastName;
    private String company;
    private String phone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ContactStatus status = ContactStatus.ACTIVE;

    /**
     * FIX PRINCIPAL :
     *
     * AVANT (causait LazyInitializationException) :
     *   @ManyToOne(fetch = FetchType.LAZY)
     *   @JsonIgnoreProperties({"contacts"})
     *   private ContactList contactList;
     *
     * APRÈS : @JsonIgnore sur l'objet entier + champ virtuel contactListId/Name
     *
     * Jackson ne touche PLUS au proxy Hibernate de ContactList.
     * On expose juste l'ID et le nom via @JsonProperty.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_list_id")
    @JsonIgnore
    private ContactList contactList;

    /**
     * Expose l'ID de la liste dans le JSON sans toucher le proxy LAZY.
     */
    @JsonProperty("contactListId")
    public Long getContactListId() {
        return contactList != null ? contactList.getId() : null;
    }

    /**
     * Expose le nom de la liste — protégé contre LazyInit.
     */
    @JsonProperty("contactListName")
    public String getContactListName() {
        try {
            return contactList != null ? contactList.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime unsubscribedAt;
    private LocalDateTime bouncedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName);
        if (lastName != null && !lastName.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(lastName);
        }
        return sb.isEmpty() ? email : sb.toString();
    }
}