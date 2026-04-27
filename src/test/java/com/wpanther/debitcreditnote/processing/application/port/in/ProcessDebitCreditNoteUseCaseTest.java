package com.wpanther.debitcreditnote.processing.application.port.in;

import com.wpanther.saga.domain.enums.SagaStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessDebitCreditNoteUseCaseTest {

    @Test
    void processException_hasMessage() {
        ProcessDebitCreditNoteUseCase.ProcessingException ex =
                new ProcessDebitCreditNoteUseCase.ProcessingException("test error");
        assertEquals("test error", ex.getMessage());
    }

    @Test
    void processException_hasCause() {
        RuntimeException cause = new RuntimeException("root cause");
        ProcessDebitCreditNoteUseCase.ProcessingException ex =
                new ProcessDebitCreditNoteUseCase.ProcessingException("wrapped", cause);
        assertEquals("wrapped", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}