package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProcessedDebitCreditNote aggregate root
 */
class ProcessedDebitCreditNoteTest {

    private ProcessedDebitCreditNote.Builder validBuilder;

    @BeforeEach
    void setUp() {
        Party seller = Party.of(
            "Acme Corporation",
            TaxIdentifier.of("1234567890", "VAT"),
            Address.of("123 Street", "Bangkok", "10110", "TH"),
            null
        );

        Party buyer = Party.of(
            "Customer Company",
            TaxIdentifier.of("9876543210", "VAT"),
            Address.of("456 Road", "Chiang Mai", "50000", "TH"),
            null
        );

        LineItem item1 = new LineItem(
            "Service 1",
            10,
            Money.of(new BigDecimal("1000.00"), "THB"),
            new BigDecimal("7.00")
        );

        validBuilder = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("DN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller)
            .buyer(buyer)
            .addItem(item1)
            .currency("THB")
            .originalXml("<xml>test</xml>");
    }

    @Test
    void build_createsNoteInPendingStatus() {
        // When
        ProcessedDebitCreditNote note = validBuilder.build();

        // Then
        assertNotNull(note);
        assertEquals("intake-123", note.getSourceNoteId());
        assertEquals("DN-001", note.getNoteNumber());
        assertEquals("DEBIT", note.getNoteType());
        assertEquals(LocalDate.of(2025, 1, 1), note.getIssueDate());
        assertEquals(LocalDate.of(2025, 2, 1), note.getDueDate());
        assertEquals("THB", note.getCurrency());
        assertEquals(ProcessingStatus.PENDING, note.getStatus());
        assertNotNull(note.getCreatedAt());
    }

    @Test
    void startProcessing_transitionsToProcessing() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();
        assertEquals(ProcessingStatus.PENDING, note.getStatus());

        // When
        note.startProcessing();

        // Then
        assertEquals(ProcessingStatus.PROCESSING, note.getStatus());
    }

    @Test
    void startProcessing_fromNonPending_throws() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();
        note.startProcessing();

        // When/Then
        assertThrows(IllegalStateException.class, note::startProcessing);
    }

    @Test
    void markCompleted_transitionsToCompleted() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();
        note.startProcessing();

        // When
        note.markCompleted();

        // Then
        assertEquals(ProcessingStatus.COMPLETED, note.getStatus());
        assertNotNull(note.getCompletedAt());
    }

    @Test
    void markCompleted_fromPending_throws() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();

        // When/Then
        assertThrows(IllegalStateException.class, () -> note.markCompleted());
    }

    @Test
    void markFailed_setsError() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();

        // When
        note.markFailed("Processing error occurred");

        // Then
        assertEquals(ProcessingStatus.FAILED, note.getStatus());
        assertEquals("Processing error occurred", note.getErrorMessage());
        assertNotNull(note.getCompletedAt());
    }

    @Test
    void markFailed_fromProcessingState() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();
        note.startProcessing();

        // When
        note.markFailed("Unexpected error");

        // Then
        assertEquals(ProcessingStatus.FAILED, note.getStatus());
        assertEquals("Unexpected error", note.getErrorMessage());
    }

    @Test
    void markFailed_nullError() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();

        // When
        note.markFailed(null);

        // Then
        assertEquals(ProcessingStatus.FAILED, note.getStatus());
        assertNull(note.getErrorMessage());
        assertNotNull(note.getCompletedAt());
    }

    @Test
    void getSubtotal_sumsLineTotals() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();

        // When
        Money subtotal = note.getSubtotal();

        // Then
        // 10 * 1000 = 10,000
        assertEquals(Money.of(new BigDecimal("10000.00"), "THB"), subtotal);
    }

    @Test
    void getTotalTax_sumsTaxAmounts() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();

        // When
        Money totalTax = note.getTotalTax();

        // Then
        // 10,000 * 0.07 = 700 (scale 4 from tax calculation: 700.0000)
        assertEquals(0, new BigDecimal("700.00").compareTo(totalTax.amount()));
        assertEquals("THB", totalTax.currency());
    }

    @Test
    void getTotal_returnsSubtotalPlusTax() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();

        // When
        Money total = note.getTotal();

        // Then
        // 10,000 + 700.0000 = 10700.0000
        assertEquals(0, new BigDecimal("10700.00").compareTo(total.amount()));
        assertEquals("THB", total.currency());
    }

    @Test
    void calculateTotals_multipleItems() {
        // Given
        LineItem item2 = new LineItem(
            "Service 2",
            5,
            Money.of(new BigDecimal("2000.00"), "THB"),
            new BigDecimal("7.00")
        );

        ProcessedDebitCreditNote note = validBuilder
            .addItem(item2)
            .build();

        // When
        Money subtotal = note.getSubtotal();
        Money totalTax = note.getTotalTax();
        Money total = note.getTotal();

        // Then
        // Subtotal: (10 * 1000) + (5 * 2000) = 20,000
        assertEquals(0, new BigDecimal("20000.00").compareTo(subtotal.amount()));
        // Tax: 20,000 * 0.07 = 1,400 (scale 4: 1400.0000)
        assertEquals(0, new BigDecimal("1400.00").compareTo(totalTax.amount()));
        // Total: 20,000 + 1,400.0000 = 21,400.0000
        assertEquals(0, new BigDecimal("21400.00").compareTo(total.amount()));
    }

    @Test
    void build_emptyItems_throws() {
        // When/Then
        assertThrows(IllegalStateException.class, () ->
            validBuilder.items(new ArrayList<>()).build()
        );
    }

    @Test
    void build_dueDateBeforeIssueDate_throws() {
        // When/Then
        assertThrows(IllegalStateException.class, () ->
            validBuilder
                .issueDate(LocalDate.of(2025, 2, 1))
                .dueDate(LocalDate.of(2025, 1, 1))
                .build()
        );
    }

    @Test
    void build_currencyMismatch_throws() {
        // Given
        LineItem itemWithDifferentCurrency = new LineItem(
            "Service",
            10,
            Money.of(new BigDecimal("1000.00"), "USD"),
            new BigDecimal("7.00")
        );

        // When/Then
        assertThrows(IllegalStateException.class, () ->
            validBuilder
                .items(List.of(itemWithDifferentCurrency))
                .build()
        );
    }

    @Test
    void build_invalidCurrencyLength_throws() {
        // When/Then
        assertThrows(IllegalStateException.class, () ->
            validBuilder.currency("US").build()
        );

        assertThrows(IllegalStateException.class, () ->
            validBuilder.currency("USDT").build()
        );
    }

    @Test
    void getItems_returnsUnmodifiableList() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();

        // When
        List<LineItem> items = note.getItems();

        // Then
        assertThrows(UnsupportedOperationException.class, () ->
            items.add(new LineItem("New Item", 1, Money.of(new BigDecimal("100.00"), "THB"), BigDecimal.ZERO))
        );
    }

    @Test
    void build_statusOverride() {
        // Given
        ProcessedDebitCreditNote note = validBuilder
            .status(ProcessingStatus.PROCESSING)
            .build();

        // When/Then
        assertEquals(ProcessingStatus.PROCESSING, note.getStatus());
    }

    @Test
    void build_customCreatedAt() {
        // Given
        LocalDateTime customCreatedAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        ProcessedDebitCreditNote note = validBuilder
            .createdAt(customCreatedAt)
            .build();

        // When/Then
        assertEquals(customCreatedAt, note.getCreatedAt());
    }

    @Test
    void build_customCompletedAt() {
        // Given
        LocalDateTime completedAt = LocalDateTime.of(2025, 1, 1, 12, 0);
        ProcessedDebitCreditNote note = validBuilder
            .completedAt(completedAt)
            .build();

        // When/Then
        assertEquals(completedAt, note.getCompletedAt());
    }

    @Test
    void build_errorMessage() {
        // Given
        ProcessedDebitCreditNote note = validBuilder
            .errorMessage("Initial error")
            .build();

        // When/Then
        assertEquals("Initial error", note.getErrorMessage());
    }

    @Test
    void completeWorkflow() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();

        // When/Then
        assertEquals(ProcessingStatus.PENDING, note.getStatus());

        note.startProcessing();
        assertEquals(ProcessingStatus.PROCESSING, note.getStatus());

        note.markCompleted();
        assertEquals(ProcessingStatus.COMPLETED, note.getStatus());
        assertNotNull(note.getCompletedAt());
    }

    @Test
    void cachedTotals() {
        // Given
        ProcessedDebitCreditNote note = validBuilder.build();

        // When
        Money subtotal1 = note.getSubtotal();
        Money subtotal2 = note.getSubtotal();
        Money tax1 = note.getTotalTax();
        Money tax2 = note.getTotalTax();
        Money total1 = note.getTotal();
        Money total2 = note.getTotal();

        // Then
        assertSame(subtotal1, subtotal2);
        assertSame(tax1, tax2);
        assertSame(total1, total2);
    }

    @Test
    void nullId_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.id(null).build()
        );
    }

    @Test
    void nullSourceNoteId_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.sourceNoteId(null).build()
        );
    }

    @Test
    void nullNoteNumber_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.noteNumber(null).build()
        );
    }

    @Test
    void nullNoteType_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.noteType(null).build()
        );
    }

    @Test
    void nullIssueDate_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.issueDate(null).build()
        );
    }

    @Test
    void nullDueDate_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.dueDate(null).build()
        );
    }

    @Test
    void nullSeller_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.seller(null).build()
        );
    }

    @Test
    void nullBuyer_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.buyer(null).build()
        );
    }

    @Test
    void nullCurrency_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.currency(null).build()
        );
    }

    @Test
    void nullOriginalXml_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            validBuilder.originalXml(null).build()
        );
    }

    @Test
    void allGetters() {
        // Given
        DebitCreditNoteId id = DebitCreditNoteId.generate();
        LocalDate issueDate = LocalDate.of(2025, 1, 1);
        LocalDate dueDate = LocalDate.of(2025, 2, 1);

        ProcessedDebitCreditNote note = validBuilder
            .id(id)
            .issueDate(issueDate)
            .dueDate(dueDate)
            .build();

        // When/Then
        assertEquals(id, note.getId());
        assertEquals("intake-123", note.getSourceNoteId());
        assertEquals("DN-001", note.getNoteNumber());
        assertEquals("DEBIT", note.getNoteType());
        assertEquals(issueDate, note.getIssueDate());
        assertEquals(dueDate, note.getDueDate());
        assertNotNull(note.getSeller());
        assertNotNull(note.getBuyer());
        assertEquals(1, note.getItems().size());
        assertEquals("THB", note.getCurrency());
        assertEquals("<xml>test</xml>", note.getOriginalXml());
        assertEquals(ProcessingStatus.PENDING, note.getStatus());
        assertNotNull(note.getCreatedAt());
        assertNull(note.getCompletedAt());
        assertNull(note.getErrorMessage());
    }
}
