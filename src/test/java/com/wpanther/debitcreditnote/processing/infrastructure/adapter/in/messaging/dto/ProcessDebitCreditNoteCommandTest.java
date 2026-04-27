package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto;

import com.wpanther.saga.domain.enums.SagaStep;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessDebitCreditNoteCommandTest {

    @Test
    void convenienceConstructor_setsAllFields() {
        ProcessDebitCreditNoteCommand cmd = new ProcessDebitCreditNoteCommand(
                "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1",
                "doc-1", "<xml>test</xml>", "DCN-001");

        assertEquals("saga-1", cmd.getSagaId());
        assertEquals(SagaStep.PROCESS_DEBIT_CREDIT_NOTE, cmd.getSagaStep());
        assertEquals("corr-1", cmd.getCorrelationId());
        assertEquals("doc-1", cmd.getDocumentId());
        assertEquals("<xml>test</xml>", cmd.getXmlContent());
        assertEquals("DCN-001", cmd.getNoteNumber());
    }

    @Test
    void jsonCreatorConstructor_setsAllFields() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();

        ProcessDebitCreditNoteCommand cmd = new ProcessDebitCreditNoteCommand(
                eventId, now, "ProcessDebitCreditNoteCommand", 1,
                "saga-2", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-2",
                "doc-2", "<xml>test2</xml>", "DCN-002");

        assertEquals(eventId, cmd.getEventId());
        assertEquals(now, cmd.getOccurredAt());
        assertEquals("ProcessDebitCreditNoteCommand", cmd.getEventType());
        assertEquals(1, cmd.getVersion());
        assertEquals("saga-2", cmd.getSagaId());
        assertEquals("corr-2", cmd.getCorrelationId());
        assertEquals("doc-2", cmd.getDocumentId());
        assertEquals("<xml>test2</xml>", cmd.getXmlContent());
        assertEquals("DCN-002", cmd.getNoteNumber());
    }

    @Test
    void sagaStep_isProcessDebitCreditNote() {
        ProcessDebitCreditNoteCommand cmd = new ProcessDebitCreditNoteCommand(
                "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1",
                "doc-1", "<xml>test</xml>", "DCN-001");

        assertEquals(SagaStep.PROCESS_DEBIT_CREDIT_NOTE, cmd.getSagaStep());
    }
}