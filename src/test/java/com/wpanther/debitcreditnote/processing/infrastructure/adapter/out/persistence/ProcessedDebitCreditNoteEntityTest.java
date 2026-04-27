package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.ProcessingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProcessedDebitCreditNoteEntityTest {

    @Test
    void testBuilderCreateEntityWithAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        String sourceNoteId = "source-123";
        String noteNumber = "DCN-001";
        String noteType = "DEBIT";
        LocalDate issueDate = LocalDate.of(2025, 1, 1);
        LocalDate dueDate = LocalDate.of(2025, 2, 1);
        String currency = "THB";
        BigDecimal subtotal = new BigDecimal("1000.00");
        BigDecimal totalTax = new BigDecimal("70.00");
        BigDecimal total = new BigDecimal("1070.00");
        String originalXml = "<xml>test</xml>";
        ProcessingStatus status = ProcessingStatus.COMPLETED;
        String errorMessage = "Test error";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime completedAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        // When
        ProcessedDebitCreditNoteEntity entity = ProcessedDebitCreditNoteEntity.builder()
            .id(id)
            .sourceNoteId(sourceNoteId)
            .noteNumber(noteNumber)
            .noteType(noteType)
            .issueDate(issueDate)
            .dueDate(dueDate)
            .currency(currency)
            .subtotal(subtotal)
            .totalTax(totalTax)
            .total(total)
            .originalXml(originalXml)
            .status(status)
            .errorMessage(errorMessage)
            .createdAt(createdAt)
            .completedAt(completedAt)
            .updatedAt(updatedAt)
            .parties(new HashSet<>())
            .lineItems(new ArrayList<>())
            .build();

        // Then
        assertEquals(id, entity.getId());
        assertEquals(sourceNoteId, entity.getSourceNoteId());
        assertEquals(noteNumber, entity.getNoteNumber());
        assertEquals(noteType, entity.getNoteType());
        assertEquals(issueDate, entity.getIssueDate());
        assertEquals(dueDate, entity.getDueDate());
        assertEquals(currency, entity.getCurrency());
        assertEquals(subtotal, entity.getSubtotal());
        assertEquals(totalTax, entity.getTotalTax());
        assertEquals(total, entity.getTotal());
        assertEquals(originalXml, entity.getOriginalXml());
        assertEquals(status, entity.getStatus());
        assertEquals(errorMessage, entity.getErrorMessage());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(completedAt, entity.getCompletedAt());
        assertEquals(updatedAt, entity.getUpdatedAt());
        assertNotNull(entity.getParties());
        assertNotNull(entity.getLineItems());
    }

    @Test
    void testMutableFieldSetters() {
        ProcessedDebitCreditNoteEntity entity = ProcessedDebitCreditNoteEntity.builder()
            .id(UUID.randomUUID())
            .sourceNoteId("source-123")
            .noteNumber("DCN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .currency("THB")
            .subtotal(new BigDecimal("1000.00"))
            .totalTax(new BigDecimal("70.00"))
            .total(new BigDecimal("1070.00"))
            .originalXml("<xml>test</xml>")
            .status(ProcessingStatus.PENDING)
            .build();

        LocalDateTime completedAt = LocalDateTime.now();
        entity.setStatus(ProcessingStatus.COMPLETED);
        entity.setCompletedAt(completedAt);
        entity.setErrorMessage("Error");

        assertEquals(ProcessingStatus.COMPLETED, entity.getStatus());
        assertEquals(completedAt, entity.getCompletedAt());
        assertEquals("Error", entity.getErrorMessage());
    }

    @Test
    void testAddParty() {
        ProcessedDebitCreditNoteEntity entity = ProcessedDebitCreditNoteEntity.builder()
            .parties(new HashSet<>())
            .build();
        DebitCreditNotePartyEntity party = DebitCreditNotePartyEntity.builder()
            .partyType("SELLER")
            .name("Test Company")
            .build();

        entity.addParty(party);

        assertEquals(1, entity.getParties().size());
        assertEquals(entity, party.getNote());
    }

    @Test
    void testAddLineItem() {
        ProcessedDebitCreditNoteEntity entity = ProcessedDebitCreditNoteEntity.builder()
            .lineItems(new ArrayList<>())
            .build();
        DebitCreditNoteLineItemEntity lineItem = DebitCreditNoteLineItemEntity.builder()
            .lineNumber(1)
            .description("Test item")
            .build();

        entity.addLineItem(lineItem);

        assertEquals(1, entity.getLineItems().size());
        assertEquals(entity, lineItem.getNote());
    }

    @Test
    void testAllArgsConstructor() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ProcessedDebitCreditNoteEntity entity = new ProcessedDebitCreditNoteEntity(
            id,
            "source-123",
            "DCN-001",
            "CREDIT",
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 2, 1),
            "THB",
            new BigDecimal("1000.00"),
            new BigDecimal("70.00"),
            new BigDecimal("1070.00"),
            "<xml>test</xml>",
            ProcessingStatus.PENDING,
            null,
            now,
            null,
            now,
            new HashSet<>(),
            new ArrayList<>()
        );

        assertEquals(id, entity.getId());
        assertEquals("source-123", entity.getSourceNoteId());
        assertEquals("DCN-001", entity.getNoteNumber());
        assertEquals("CREDIT", entity.getNoteType());
    }

    @Test
    void testNoArgsConstructor() {
        ProcessedDebitCreditNoteEntity entity = new ProcessedDebitCreditNoteEntity();

        assertNotNull(entity);
        assertNull(entity.getId());
        assertNull(entity.getNoteNumber());
    }

    @Test
    void testBuilderDefaults() {
        ProcessedDebitCreditNoteEntity entity = ProcessedDebitCreditNoteEntity.builder().build();

        assertNotNull(entity.getParties());
        assertTrue(entity.getParties().isEmpty());
        assertNotNull(entity.getLineItems());
        assertTrue(entity.getLineItems().isEmpty());
    }
}
