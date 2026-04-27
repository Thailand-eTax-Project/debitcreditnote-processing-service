package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "debit_credit_note_parties", indexes = {
    @Index(name = "idx_party_note_id", columnList = "note_id"),
    @Index(name = "idx_party_type", columnList = "party_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitCreditNotePartyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false, foreignKey = @ForeignKey(name = "fk_party_note"))
    private ProcessedDebitCreditNoteEntity note;

    @Column(name = "party_type", nullable = false, length = 20)
    private String partyType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "tax_id", nullable = false, length = 50)
    private String taxId;

    @Column(name = "tax_scheme", nullable = false, length = 20)
    private String taxScheme;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "street_address", length = 500)
    private String streetAddress;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 3)
    private String country;

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now();
        }
    }
}
