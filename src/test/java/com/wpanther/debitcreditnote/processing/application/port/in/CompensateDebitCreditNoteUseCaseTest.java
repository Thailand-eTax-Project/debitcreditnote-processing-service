package com.wpanther.debitcreditnote.processing.application.port.in;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompensateDebitCreditNoteUseCaseTest {

    @Test
    void compensationException_hasMessage() {
        CompensateDebitCreditNoteUseCase.CompensationException ex =
                new CompensateDebitCreditNoteUseCase.CompensationException("rollback failed");
        assertEquals("rollback failed", ex.getMessage());
    }

    @Test
    void compensationException_hasCause() {
        RuntimeException cause = new RuntimeException("db error");
        CompensateDebitCreditNoteUseCase.CompensationException ex =
                new CompensateDebitCreditNoteUseCase.CompensationException("wrapped", cause);
        assertEquals("wrapped", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}