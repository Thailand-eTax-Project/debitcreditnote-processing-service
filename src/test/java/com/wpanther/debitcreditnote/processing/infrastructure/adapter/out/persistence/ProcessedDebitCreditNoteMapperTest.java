package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessedDebitCreditNoteMapperTest {

    private ProcessedDebitCreditNoteMapper mapper;
    private ProcessedDebitCreditNote domainNote;

    @BeforeEach
    void setUp() {
        mapper = new ProcessedDebitCreditNoteMapper();

        Party seller = Party.of(
            "Seller Company",
            TaxIdentifier.of("1234567890", "VAT"),
            Address.of("123 Street", "Bangkok", "10110", "TH"),
            "seller@company.com"
        );

        Party buyer = Party.of(
            "Buyer Company",
            TaxIdentifier.of("9876543210", "VAT"),
            Address.of("456 Road", "Chiang Mai", "50000", "TH"),
            "buyer@company.com"
        );

        LineItem item1 = new LineItem(
            "Service 1",
            10,
            Money.of(new BigDecimal("1000.00"), "THB"),
            new BigDecimal("7.00")
        );

        LineItem item2 = new LineItem(
            "Service 2",
            5,
            Money.of(new BigDecimal("2000.00"), "THB"),
            new BigDecimal("7.00")
        );

        domainNote = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("DCN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller)
            .buyer(buyer)
            .addItem(item1)
            .addItem(item2)
            .currency("THB")
            .originalXml("<xml>test</xml>")
            .status(ProcessingStatus.PENDING)
            .createdAt(LocalDateTime.of(2025, 1, 1, 10, 0))
            .build();
    }

    @Test
    void testToEntity() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);

        assertNotNull(entity);
        assertEquals(domainNote.getId().getValue(), entity.getId());
        assertEquals("intake-123", entity.getSourceNoteId());
        assertEquals("DCN-001", entity.getNoteNumber());
        assertEquals("DEBIT", entity.getNoteType());
        assertEquals(LocalDate.of(2025, 1, 1), entity.getIssueDate());
        assertEquals(LocalDate.of(2025, 2, 1), entity.getDueDate());
        assertEquals("THB", entity.getCurrency());
        assertEquals(ProcessingStatus.PENDING, entity.getStatus());
        assertEquals("<xml>test</xml>", entity.getOriginalXml());
    }

    @Test
    void testToEntityCalculatedTotals() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);

        // Subtotal: (10 * 1000) + (5 * 2000) = 20,000
        assertEquals(0, new BigDecimal("20000.00").compareTo(entity.getSubtotal()));
        // Tax: 20,000 * 0.07 = 1,400
        assertEquals(0, new BigDecimal("1400.00").compareTo(entity.getTotalTax()));
        // Total: 20,000 + 1,400 = 21,400
        assertEquals(0, new BigDecimal("21400.00").compareTo(entity.getTotal()));
    }

    @Test
    void testToEntityParties() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);

        assertNotNull(entity.getParties());
        assertEquals(2, entity.getParties().size());

        DebitCreditNotePartyEntity seller = entity.getParties().stream()
            .filter(p -> "SELLER".equals(p.getPartyType()))
            .findFirst()
            .orElse(null);

        DebitCreditNotePartyEntity buyer = entity.getParties().stream()
            .filter(p -> "BUYER".equals(p.getPartyType()))
            .findFirst()
            .orElse(null);

        assertNotNull(seller);
        assertEquals("Seller Company", seller.getName());
        assertEquals("1234567890", seller.getTaxId());
        assertEquals("VAT", seller.getTaxScheme());
        assertEquals("123 Street", seller.getStreetAddress());
        assertEquals("Bangkok", seller.getCity());
        assertEquals("10110", seller.getPostalCode());
        assertEquals("TH", seller.getCountry());
        assertEquals("seller@company.com", seller.getEmail());

        assertNotNull(buyer);
        assertEquals("Buyer Company", buyer.getName());
        assertEquals("9876543210", buyer.getTaxId());
    }

    @Test
    void testToEntityLineItems() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);

        assertNotNull(entity.getLineItems());
        assertEquals(2, entity.getLineItems().size());

        DebitCreditNoteLineItemEntity item1 = entity.getLineItems().get(0);
        assertEquals(1, item1.getLineNumber());
        assertEquals("Service 1", item1.getDescription());
        assertEquals(10, item1.getQuantity());
        assertEquals(0, new BigDecimal("1000.00").compareTo(item1.getUnitPrice()));
        assertEquals(0, new BigDecimal("7.00").compareTo(item1.getTaxRate()));
        assertEquals(0, new BigDecimal("10000.00").compareTo(item1.getLineTotal()));
        assertEquals(0, new BigDecimal("700.00").compareTo(item1.getTaxAmount()));

        DebitCreditNoteLineItemEntity item2 = entity.getLineItems().get(1);
        assertEquals(2, item2.getLineNumber());
        assertEquals("Service 2", item2.getDescription());
    }

    @Test
    void testToDomain() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);
        ProcessedDebitCreditNote reconstructed = mapper.toDomain(entity);

        assertNotNull(reconstructed);
        assertEquals(domainNote.getId(), reconstructed.getId());
        assertEquals(domainNote.getSourceNoteId(), reconstructed.getSourceNoteId());
        assertEquals(domainNote.getNoteNumber(), reconstructed.getNoteNumber());
        assertEquals(domainNote.getNoteType(), reconstructed.getNoteType());
        assertEquals(domainNote.getIssueDate(), reconstructed.getIssueDate());
        assertEquals(domainNote.getDueDate(), reconstructed.getDueDate());
        assertEquals(domainNote.getCurrency(), reconstructed.getCurrency());
        assertEquals(domainNote.getStatus(), reconstructed.getStatus());
        assertEquals(domainNote.getOriginalXml(), reconstructed.getOriginalXml());
    }

    @Test
    void testToDomainParties() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);
        ProcessedDebitCreditNote reconstructed = mapper.toDomain(entity);

        assertNotNull(reconstructed.getSeller());
        assertEquals("Seller Company", reconstructed.getSeller().name());
        assertEquals("1234567890", reconstructed.getSeller().taxIdentifier().taxId());
        assertEquals("VAT", reconstructed.getSeller().taxIdentifier().scheme());
        assertEquals("123 Street", reconstructed.getSeller().address().street());
        assertEquals("Bangkok", reconstructed.getSeller().address().city());
        assertEquals("10110", reconstructed.getSeller().address().postalCode());
        assertEquals("TH", reconstructed.getSeller().address().country());
        assertEquals("seller@company.com", reconstructed.getSeller().email());

        assertNotNull(reconstructed.getBuyer());
        assertEquals("Buyer Company", reconstructed.getBuyer().name());
        assertEquals("9876543210", reconstructed.getBuyer().taxIdentifier().taxId());
    }

    @Test
    void testToDomainLineItems() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);
        ProcessedDebitCreditNote reconstructed = mapper.toDomain(entity);

        assertNotNull(reconstructed.getItems());
        assertEquals(2, reconstructed.getItems().size());

        LineItem item1 = reconstructed.getItems().get(0);
        assertEquals("Service 1", item1.description());
        assertEquals(10, item1.quantity());
        assertEquals(Money.of(new BigDecimal("1000.00"), "THB"), item1.unitPrice());
        assertEquals(new BigDecimal("7.00"), item1.taxRate());

        LineItem item2 = reconstructed.getItems().get(1);
        assertEquals("Service 2", item2.description());
        assertEquals(5, item2.quantity());
    }

    @Test
    void testRoundTripConversion() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);
        ProcessedDebitCreditNote reconstructed = mapper.toDomain(entity);

        assertEquals(domainNote.getSubtotal(), reconstructed.getSubtotal());
        assertEquals(domainNote.getTotalTax(), reconstructed.getTotalTax());
        assertEquals(domainNote.getTotal(), reconstructed.getTotal());
    }

    @Test
    void testToEntityWithCompletedStatus() {
        ProcessedDebitCreditNote completedNote = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("DCN-002")
            .noteType("CREDIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(domainNote.getSeller())
            .buyer(domainNote.getBuyer())
            .items(domainNote.getItems())
            .currency("THB")
            .originalXml("<xml>test</xml>")
            .status(ProcessingStatus.COMPLETED)
            .completedAt(LocalDateTime.of(2025, 1, 1, 12, 0))
            .build();

        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(completedNote);

        assertEquals(ProcessingStatus.COMPLETED, entity.getStatus());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), entity.getCompletedAt());
    }

    @Test
    void testToEntityWithErrorMessage() {
        ProcessedDebitCreditNote noteWithError = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("DCN-003")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(domainNote.getSeller())
            .buyer(domainNote.getBuyer())
            .items(domainNote.getItems())
            .currency("THB")
            .originalXml("<xml>test</xml>")
            .status(ProcessingStatus.PENDING)
            .errorMessage("Test error message")
            .build();

        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(noteWithError);

        assertEquals(ProcessingStatus.PENDING, entity.getStatus());
        assertEquals("Test error message", entity.getErrorMessage());
    }

    @Test
    void testToDomainReconstructsCorrectly() {
        ProcessedDebitCreditNoteEntity entity1 = mapper.toEntity(domainNote);
        ProcessedDebitCreditNote domain = mapper.toDomain(entity1);
        ProcessedDebitCreditNoteEntity entity2 = mapper.toEntity(domain);

        assertEquals(entity1.getId(), entity2.getId());
        assertEquals(entity1.getNoteNumber(), entity2.getNoteNumber());
        assertEquals(entity1.getNoteType(), entity2.getNoteType());
        assertEquals(entity1.getSubtotal(), entity2.getSubtotal());
        assertEquals(entity1.getTotalTax(), entity2.getTotalTax());
        assertEquals(entity1.getTotal(), entity2.getTotal());
    }

    @Test
    void testLineItemNumbering() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);

        List<DebitCreditNoteLineItemEntity> items = entity.getLineItems();
        for (int i = 0; i < items.size(); i++) {
            assertEquals(i + 1, items.get(i).getLineNumber());
        }
    }

    @Test
    void toDomain_whenNoSellerParty_throwsIllegalStateException() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);
        entity.getParties().removeIf(p -> "SELLER".equals(p.getPartyType()));

        assertThrows(IllegalStateException.class, () -> mapper.toDomain(entity));
    }

    @Test
    void toDomain_whenNoBuyerParty_throwsIllegalStateException() {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(domainNote);
        entity.getParties().removeIf(p -> "BUYER".equals(p.getPartyType()));

        assertThrows(IllegalStateException.class, () -> mapper.toDomain(entity));
    }
}
