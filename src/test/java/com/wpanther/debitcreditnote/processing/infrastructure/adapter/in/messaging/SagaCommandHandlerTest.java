package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging;

import com.wpanther.debitcreditnote.processing.application.port.in.CompensateDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.CompensateDebitCreditNoteCommand;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.ProcessDebitCreditNoteCommand;
import com.wpanther.saga.domain.enums.SagaStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SagaCommandHandler.
 * Tests that the handler correctly delegates to use cases.
 */
@ExtendWith(MockitoExtension.class)
class SagaCommandHandlerTest {

    @Mock
    private ProcessDebitCreditNoteUseCase processUseCase;

    @Mock
    private CompensateDebitCreditNoteUseCase compensateUseCase;

    @InjectMocks
    private SagaCommandHandler sagaCommandHandler;

    private ProcessDebitCreditNoteCommand validCommand;
    private CompensateDebitCreditNoteCommand compensateCommand;

    @BeforeEach
    void setUp() {
        validCommand = new ProcessDebitCreditNoteCommand(
            "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1",
            "doc-001", "<xml/>", "DN-001"
        );

        compensateCommand = new CompensateDebitCreditNoteCommand(
            "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1",
            "process-debit-credit-note", "doc-001", "debit-credit-note"
        );
    }

    @Test
    void shouldDelegateToProcessUseCase() throws Exception {
        // Given
        doNothing().when(processUseCase).process(any(), any(), any(), any(), any());

        // When
        sagaCommandHandler.handleProcessCommand(validCommand);

        // Then
        verify(processUseCase).process(
            eq("doc-001"),
            eq("<xml/>"),
            eq("saga-1"),
            eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE),
            eq("corr-1")
        );
        verify(compensateUseCase, never()).compensate(any(), any(), any(), any());
    }

    @Test
    void shouldDelegateToCompensateUseCase() throws Exception {
        // Given
        doNothing().when(compensateUseCase).compensate(any(), any(), any(), any());

        // When
        sagaCommandHandler.handleCompensation(compensateCommand);

        // Then
        verify(compensateUseCase).compensate(
            eq("doc-001"),
            eq("saga-1"),
            eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE),
            eq("corr-1")
        );
        verify(processUseCase, never()).process(any(), any(), any(), any(), any());
    }

    @Test
    void shouldCatchProcessingException() throws Exception {
        // Given
        doThrow(new ProcessDebitCreditNoteUseCase.ProcessingException("Processing error"))
            .when(processUseCase).process(any(), any(), any(), any(), any());

        // When - exception is caught and logged by handler
        assertDoesNotThrow(() -> sagaCommandHandler.handleProcessCommand(validCommand));

        verify(processUseCase).process(any(), any(), any(), any(), any());
    }

    @Test
    void shouldPropagateCompensationException() throws Exception {
        // Given - use case publishes FAILURE reply then throws CompensationException
        doThrow(new CompensateDebitCreditNoteUseCase.CompensationException(
                "Compensation failed", new RuntimeException("DB error")))
            .when(compensateUseCase).compensate(any(), any(), any(), any());

        // When/Then - exception propagates to Camel so the DLC can retry
        assertThrows(CompensateDebitCreditNoteUseCase.CompensationException.class,
            () -> sagaCommandHandler.handleCompensation(compensateCommand));

        verify(compensateUseCase).compensate(any(), any(), any(), any());
    }
}
