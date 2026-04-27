package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DebitCreditNotePartyEntityTest {

    @Test
    void testBuilderCreateEntityWithAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        DebitCreditNotePartyEntity entity = DebitCreditNotePartyEntity.builder()
            .id(id)
            .partyType("SELLER")
            .name("Test Company")
            .taxId("1234567890")
            .taxScheme("VAT")
            .email("test@company.com")
            .streetAddress("123 Street")
            .city("Bangkok")
            .postalCode("10110")
            .country("TH")
            .createdAt(createdAt)
            .build();

        assertEquals(id, entity.getId());
        assertEquals("SELLER", entity.getPartyType());
        assertEquals("Test Company", entity.getName());
        assertEquals("1234567890", entity.getTaxId());
        assertEquals("VAT", entity.getTaxScheme());
        assertEquals("test@company.com", entity.getEmail());
        assertEquals("123 Street", entity.getStreetAddress());
        assertEquals("Bangkok", entity.getCity());
        assertEquals("10110", entity.getPostalCode());
        assertEquals("TH", entity.getCountry());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void testAllArgsConstructor() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        DebitCreditNotePartyEntity entity = new DebitCreditNotePartyEntity(
            id,
            null,
            "BUYER",
            "Buyer Corp",
            "9876543210",
            "VAT",
            "buyer@corp.com",
            "456 Road",
            "Chiang Mai",
            "50000",
            "TH",
            createdAt
        );

        assertEquals(id, entity.getId());
        assertNull(entity.getNote());
        assertEquals("BUYER", entity.getPartyType());
        assertEquals("Buyer Corp", entity.getName());
    }

    @Test
    void testNoArgsConstructor() {
        DebitCreditNotePartyEntity entity = new DebitCreditNotePartyEntity();

        assertNotNull(entity);
        assertNull(entity.getId());
        assertNull(entity.getName());
    }

    @Test
    void testPrePersistGeneratesId() {
        DebitCreditNotePartyEntity entity = DebitCreditNotePartyEntity.builder().build();
        assertNull(entity.getId());

        entity.onCreate();

        assertNotNull(entity.getId());
    }

    @Test
    void testPrePersistDoesNotOverrideExistingId() {
        UUID id = UUID.randomUUID();
        DebitCreditNotePartyEntity entity = DebitCreditNotePartyEntity.builder()
            .id(id)
            .build();

        entity.onCreate();

        assertEquals(id, entity.getId());
    }

    @Test
    void testPrePersistGeneratesCreatedAt() {
        DebitCreditNotePartyEntity entity = DebitCreditNotePartyEntity.builder().build();
        assertNull(entity.getCreatedAt());

        entity.onCreate();

        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void testPrePersistDoesNotOverrideExistingCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        DebitCreditNotePartyEntity entity = DebitCreditNotePartyEntity.builder()
            .createdAt(createdAt)
            .build();

        entity.onCreate();

        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void testNoteAssociation() {
        ProcessedDebitCreditNoteEntity note = ProcessedDebitCreditNoteEntity.builder()
            .id(UUID.randomUUID())
            .build();
        DebitCreditNotePartyEntity entity = DebitCreditNotePartyEntity.builder()
            .note(note)
            .build();

        assertEquals(note, entity.getNote());
    }

    @Test
    void testMutableFields() {
        DebitCreditNotePartyEntity entity = DebitCreditNotePartyEntity.builder()
            .name("Original Name")
            .email("original@email.com")
            .build();

        entity.setName("Updated Name");
        entity.setEmail("updated@email.com");

        assertEquals("Updated Name", entity.getName());
        assertEquals("updated@email.com", entity.getEmail());
    }
}
