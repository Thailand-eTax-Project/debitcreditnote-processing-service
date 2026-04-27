package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto;

import com.wpanther.saga.domain.enums.SagaStep;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CompensateDebitCreditNoteCommandTest {

    @Test
    void convenienceConstructor_setsAllFields() {
        CompensateDebitCreditNoteCommand cmd = new CompensateDebitCreditNoteCommand(
                "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1",
                "process-debit-credit-note", "doc-1", "debit-credit-note");

        assertEquals("saga-1", cmd.getSagaId());
        assertEquals(SagaStep.PROCESS_DEBIT_CREDIT_NOTE, cmd.getSagaStep());
        assertEquals("corr-1", cmd.getCorrelationId());
        assertEquals("process-debit-credit-note", cmd.getStepToCompensate());
        assertEquals("doc-1", cmd.getDocumentId());
        assertEquals("debit-credit-note", cmd.getDocumentType());
    }

    @Test
    void jsonCreatorConstructor_setsAllFields() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        CompensateDebitCreditNoteCommand cmd = new CompensateDebitCreditNoteCommand(
                eventId, now, "CompensateDebitCreditNoteCommand", 1,
                "saga-2", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-2",
                "process-debit-credit-note", "doc-2", "debit-credit-note");

        assertEquals(eventId, cmd.getEventId());
        assertEquals(now, cmd.getOccurredAt());
        assertEquals("CompensateDebitCreditNoteCommand", cmd.getEventType());
        assertEquals(1, cmd.getVersion());
        assertEquals("saga-2", cmd.getSagaId());
        assertEquals("corr-2", cmd.getCorrelationId());
        assertEquals("process-debit-credit-note", cmd.getStepToCompensate());
        assertEquals("doc-2", cmd.getDocumentId());
        assertEquals("debit-credit-note", cmd.getDocumentType());
    }
}