package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DebitCreditNoteLineItemEntityTest {

    @Test
    void testBuilderCreateEntityWithAllFields() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        DebitCreditNoteLineItemEntity entity = DebitCreditNoteLineItemEntity.builder()
            .id(id)
            .lineNumber(1)
            .description("Test item")
            .quantity(10)
            .unitPrice(new BigDecimal("1000.00"))
            .taxRate(new BigDecimal("7.00"))
            .lineTotal(new BigDecimal("10000.00"))
            .taxAmount(new BigDecimal("700.00"))
            .totalWithTax(new BigDecimal("10700.00"))
            .createdAt(createdAt)
            .build();

        assertEquals(id, entity.getId());
        assertEquals(1, entity.getLineNumber());
        assertEquals("Test item", entity.getDescription());
        assertEquals(10, entity.getQuantity());
        assertEquals(new BigDecimal("1000.00"), entity.getUnitPrice());
        assertEquals(new BigDecimal("7.00"), entity.getTaxRate());
        assertEquals(new BigDecimal("10000.00"), entity.getLineTotal());
        assertEquals(new BigDecimal("700.00"), entity.getTaxAmount());
        assertEquals(new BigDecimal("10700.00"), entity.getTotalWithTax());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void testAllArgsConstructor() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        DebitCreditNoteLineItemEntity entity = new DebitCreditNoteLineItemEntity(
            id,
            null,
            2,
            "Service description",
            5,
            new BigDecimal("2000.00"),
            new BigDecimal("7.00"),
            new BigDecimal("10000.00"),
            new BigDecimal("700.00"),
            new BigDecimal("10700.00"),
            createdAt
        );

        assertEquals(id, entity.getId());
        assertNull(entity.getNote());
        assertEquals(2, entity.getLineNumber());
        assertEquals("Service description", entity.getDescription());
        assertEquals(5, entity.getQuantity());
    }

    @Test
    void testNoArgsConstructor() {
        DebitCreditNoteLineItemEntity entity = new DebitCreditNoteLineItemEntity();

        assertNotNull(entity);
        assertNull(entity.getId());
        assertNull(entity.getDescription());
    }

    @Test
    void testPrePersistGeneratesId() {
        DebitCreditNoteLineItemEntity entity = DebitCreditNoteLineItemEntity.builder().build();
        assertNull(entity.getId());

        entity.onCreate();

        assertNotNull(entity.getId());
    }

    @Test
    void testPrePersistDoesNotOverrideExistingId() {
        UUID id = UUID.randomUUID();
        DebitCreditNoteLineItemEntity entity = DebitCreditNoteLineItemEntity.builder()
            .id(id)
            .build();

        entity.onCreate();

        assertEquals(id, entity.getId());
    }

    @Test
    void testPrePersistGeneratesCreatedAt() {
        DebitCreditNoteLineItemEntity entity = DebitCreditNoteLineItemEntity.builder().build();
        assertNull(entity.getCreatedAt());

        entity.onCreate();

        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void testPrePersistDoesNotOverrideExistingCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        DebitCreditNoteLineItemEntity entity = DebitCreditNoteLineItemEntity.builder()
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
        DebitCreditNoteLineItemEntity entity = DebitCreditNoteLineItemEntity.builder()
            .note(note)
            .build();

        assertEquals(note, entity.getNote());
    }

    @Test
    void testMutableFields() {
        DebitCreditNoteLineItemEntity entity = DebitCreditNoteLineItemEntity.builder()
            .description("Original")
            .quantity(1)
            .build();

        entity.setDescription("Updated");
        entity.setQuantity(20);

        assertEquals("Updated", entity.getDescription());
        assertEquals(20, entity.getQuantity());
    }
}
