package com.pfe.campaignservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pfe.campaignservice.enums.ContactStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contact_lists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    /**
     * FIX : @JsonIgnore — Jackson ne sérialise JAMAIS cette collection LAZY.
     * Les contacts sont récupérés via GET /api/contacts/list/{id}
     */
    @OneToMany(mappedBy = "contactList", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Contact> contacts = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int getTotalContactCount() {
        try {
            return contacts == null ? 0 : contacts.size();
        } catch (Exception e) {
            return 0;
        }
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int getActiveContactCount() {
        try {
            if (contacts == null) return 0;
            return (int) contacts.stream()
                    .filter(c -> c.getStatus() == ContactStatus.ACTIVE)
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }
}