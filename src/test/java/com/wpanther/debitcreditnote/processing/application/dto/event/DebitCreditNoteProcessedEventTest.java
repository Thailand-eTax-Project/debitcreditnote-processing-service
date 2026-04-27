package com.wpanther.debitcreditnote.processing.application.dto.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DebitCreditNoteProcessedEventTest {

    @Test
    void convenienceConstructor_setsAllFields() {
        DebitCreditNoteProcessedEvent event = new DebitCreditNoteProcessedEvent(
                "doc-1", "DCN-001", new BigDecimal("1000.00"),
                "THB", "saga-1", "corr-1");

        assertEquals("doc-1", event.getDocumentId());
        assertEquals("DCN-001", event.getDocumentNumber());
        assertEquals(new BigDecimal("1000.00"), event.getAmount());
        assertEquals("THB", event.getCurrency());
        assertEquals("saga-1", event.getSagaId());
        assertEquals("corr-1", event.getCorrelationId());
    }

    @Test
    void getEventType_returnsDebitcreditnoteProcessed() {
        DebitCreditNoteProcessedEvent event = new DebitCreditNoteProcessedEvent(
                "doc-1", "DCN-001", new BigDecimal("1000.00"),
                "THB", "saga-1", "corr-1");

        assertEquals("debitcreditnote.processed", event.getEventType());
    }

    @Test
    void jsonCreatorConstructor_setsAllFields() {
        DebitCreditNoteProcessedEvent event = new DebitCreditNoteProcessedEvent(
                java.util.UUID.randomUUID(),
                java.time.Instant.now(),
                "debitcreditnote.processed",
                1,
                "saga-2",
                "corr-2",
                "debitcreditnote-processing-service",
                "DEBITCREDITNOTE_PROCESSED",
                null,
                "doc-2",
                "DCN-002",
                new BigDecimal("2000.00"),
                "USD"
        );

        assertEquals("doc-2", event.getDocumentId());
        assertEquals("DCN-002", event.getDocumentNumber());
        assertEquals(new BigDecimal("2000.00"), event.getAmount());
        assertEquals("USD", event.getCurrency());
    }
}