package com.wpanther.debitcreditnote.processing.application.service;

import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.debitcreditnote.processing.application.port.in.CompensateDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.out.DebitCreditNoteEventPublishingPort;
import com.wpanther.debitcreditnote.processing.application.port.out.SagaReplyPort;
import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedDomainEvent;
import com.wpanther.debitcreditnote.processing.domain.model.*;
import com.wpanther.debitcreditnote.processing.domain.port.out.DebitCreditNoteParserPort;
import com.wpanther.debitcreditnote.processing.domain.port.out.ProcessedDebitCreditNoteRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DebitCreditNoteProcessingService (saga version)
 */
@ExtendWith(MockitoExtension.class)
class DebitCreditNoteProcessingServiceTest {

    @Mock
    private ProcessedDebitCreditNoteRepository noteRepository;

    @Mock
    private DebitCreditNoteParserPort parserService;

    @Mock
    private DebitCreditNoteEventPublishingPort eventPublisher;

    @Mock
    private SagaReplyPort sagaReplyPort;

    @Mock
    private PlatformTransactionManager transactionManager;

    private DebitCreditNoteProcessingService service;

    private ProcessedDebitCreditNote validNote;

    @BeforeEach
    void setUp() {
        service = new DebitCreditNoteProcessingService(
            noteRepository,
            parserService,
            eventPublisher,
            sagaReplyPort,
            new SimpleMeterRegistry(),
            transactionManager
        );

        Party seller = Party.of(
            "Seller Company",
            TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 Street", "Bangkok", "10110", "TH"),
            null
        );

        Party buyer = Party.of(
            "Buyer Company",
            TaxIdentifier.of("9876543210", "VAT"),
            new Address("456 Road", "Chiang Mai", "50000", "TH"),
            null
        );

        LineItem item = new LineItem(
            "Service 1",
            10,
            Money.of(new BigDecimal("1000.00"), "THB"),
            new BigDecimal("7.00")
        );

        validNote = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("DCN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller)
            .buyer(buyer)
            .addItem(item)
            .currency("THB")
            .originalXml("<xml>test</xml>")
            .build();
    }

    @Test
    void testProcessNoteForSagaSuccess() throws Exception {
        // Given
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class))).thenReturn(validNote);

        // When
        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        // Then
        verify(noteRepository).findBySourceNoteId("intake-123");
        verify(parserService).parse("<xml>test</xml>", "intake-123");
        verify(noteRepository, times(2)).save(any(ProcessedDebitCreditNote.class));
        verify(eventPublisher).publish(any(DebitCreditNoteProcessedDomainEvent.class));
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");
    }

    @Test
    void testProcessNoteForSagaAlreadyProcessed() throws Exception {
        // Given — simulate a COMPLETED note already in DB (true idempotent case)
        Party seller = Party.of("Seller Company", TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 Street", "Bangkok", "10110", "TH"), null);
        Party buyer = Party.of("Buyer Company", TaxIdentifier.of("9876543210", "VAT"),
            new Address("456 Road", "Chiang Mai", "50000", "TH"), null);
        LineItem item = new LineItem("Service 1", 10,
            Money.of(new BigDecimal("1000.00"), "THB"), new BigDecimal("7.00"));
        ProcessedDebitCreditNote completedNote = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("DCN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller).buyer(buyer).addItem(item)
            .currency("THB").originalXml("<xml>test</xml>")
            .status(ProcessingStatus.COMPLETED)
            .build();
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.of(completedNote));

        // When
        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        // Then — no re-processing; saga reply still published
        verify(noteRepository).findBySourceNoteId("intake-123");
        verify(parserService, never()).parse(anyString(), anyString());
        verify(noteRepository, never()).save(any(ProcessedDebitCreditNote.class));
        verify(eventPublisher, never()).publish(any());
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");
    }

    @Test
    void testProcessNoteForSagaResumesCompletionFromProcessingState() throws Exception {
        // Given — previous attempt saved the entity in PROCESSING state but failed before
        // completing it. The retry delivers the same command.
        Party seller = Party.of("Seller Company", TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 Street", "Bangkok", "10110", "TH"), null);
        Party buyer = Party.of("Buyer Company", TaxIdentifier.of("9876543210", "VAT"),
            new Address("456 Road", "Chiang Mai", "50000", "TH"), null);
        LineItem item = new LineItem("Service 1", 10,
            Money.of(new BigDecimal("1000.00"), "THB"), new BigDecimal("7.00"));
        ProcessedDebitCreditNote processingNote = ProcessedDebitCreditNote.builder()
            .id(DebitCreditNoteId.generate())
            .sourceNoteId("intake-123")
            .noteNumber("DCN-001")
            .noteType("DEBIT")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller).buyer(buyer).addItem(item)
            .currency("THB").originalXml("<xml>test</xml>")
            .status(ProcessingStatus.PROCESSING)
            .build();
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.of(processingNote));

        // When
        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        // Then — no re-parsing, no re-inserting; entity completed and events published
        verify(parserService, never()).parse(anyString(), anyString());
        verify(noteRepository, times(1)).save(any(ProcessedDebitCreditNote.class));
        verify(eventPublisher).publish(any(DebitCreditNoteProcessedDomainEvent.class));
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");
        // Verify the domain object was actually transitioned to COMPLETED
        assertEquals(ProcessingStatus.COMPLETED, processingNote.getStatus());
    }

    @Test
    void testProcessNoteForSagaParsingError() throws Exception {
        // Given
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString()))
            .thenThrow(new DebitCreditNoteParserPort.ParsingException("Parse error"));

        // When / Then - the service wraps ParsingException in ProcessingException
        assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
            () -> service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));

        verify(parserService).parse("<xml>test</xml>", "intake-123");
        verify(noteRepository, never()).save(any(ProcessedDebitCreditNote.class));
        verify(eventPublisher, never()).publish(any());
        // Verify failure reply is published
        verify(sagaReplyPort).publishFailure(eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("correlation-123"), contains("Parse error"));
    }

    @Test
    void testProcessNoteForSagaPublishesCorrectEvent() throws Exception {
        // Given
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class))).thenReturn(validNote);

        // When
        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        // Then
        ArgumentCaptor<DebitCreditNoteProcessedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(DebitCreditNoteProcessedDomainEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());

        DebitCreditNoteProcessedDomainEvent processedEvent = eventCaptor.getValue();
        assertEquals("DCN-001", processedEvent.documentNumber());
        assertEquals("THB", processedEvent.total().currency());
        assertEquals("correlation-123", processedEvent.correlationId());
    }

    @Test
    void testProcessNoteForSagaSavesTwice() throws Exception {
        // Given
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class))).thenReturn(validNote);

        // When
        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");

        // Then - Should save twice: PROCESSING and COMPLETED
        verify(noteRepository, times(2)).save(any(ProcessedDebitCreditNote.class));
    }

    @Test
    void testProcessNoteForSagaDatabaseError() throws Exception {
        // Given
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When / Then
        assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
            () -> service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));

        // Verify failure reply is published to avoid hanging the saga
        verify(sagaReplyPort).publishFailure(eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("correlation-123"), contains("Processing error"));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void testProcessNoteForSagaReturnsProcessedNote() throws Exception {
        // Given
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class))).thenReturn(validNote);

        // When
        service.process("intake-123", "<xml>test</xml>", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        // Then
        // Verify processing was successful by checking repository calls
        verify(noteRepository).findBySourceNoteId("intake-123");
        verify(parserService).parse("<xml>test</xml>", "intake-123");
        verify(noteRepository, times(2)).save(any(ProcessedDebitCreditNote.class));
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");
    }

    @Test
    void testCompensateDeletesExistingNote() throws Exception {
        // Given
        when(noteRepository.findBySourceNoteId("intake-123")).thenReturn(Optional.of(validNote));

        // When
        service.compensate("intake-123", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        // Then
        verify(noteRepository).findBySourceNoteId("intake-123");
        verify(noteRepository).deleteById(validNote.getId());
        verify(sagaReplyPort).publishCompensated("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");
    }

    @Test
    void testCompensateNotFound() throws Exception {
        // Given
        when(noteRepository.findBySourceNoteId("intake-notfound")).thenReturn(Optional.empty());

        // When
        service.compensate("intake-notfound", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");

        // Then
        verify(noteRepository).findBySourceNoteId("intake-notfound");
        verify(noteRepository, never()).deleteById(any());
        verify(sagaReplyPort).publishCompensated("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1");
    }

    @Test
    void testCompensateHandlesException() {
        // Given
        when(noteRepository.findBySourceNoteId("intake-123")).thenReturn(Optional.of(validNote));
        doThrow(new RuntimeException("DB error")).when(noteRepository).deleteById(any());

        // When/Then - exception is rethrown so Camel DLC can retry; FAILURE reply is still published
        assertThrows(CompensateDebitCreditNoteUseCase.CompensationException.class,
            () -> service.compensate("intake-123", "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "corr-1"));

        verify(noteRepository).findBySourceNoteId("intake-123");
        verify(noteRepository).deleteById(any());
        verify(sagaReplyPort).publishFailure(eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("corr-1"), contains("Compensation failed"));
    }

    @Test
    void testProcessNoteForSagaDataIntegrityViolationPropagates() throws Exception {
        // Given - simulate the "ghost duplicate" scenario: idempotency check passes, insert
        // conflicts on uq_processed_debit_credit_notes_source_note_id, but the REQUIRES_NEW
        // re-check finds no record (the winning thread rolled back or never committed).
        // The exception must carry a SQLException with SQLState "23505" (ANSI unique_violation)
        // matching what the PostgreSQL JDBC driver wraps inside DuplicateKeyException.
        SQLException sqlCause = new SQLException(
            "ERROR: duplicate key value violates unique constraint" +
            " \"uq_processed_debit_credit_notes_source_note_id\"", "23505");
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class)))
            .thenThrow(new DuplicateKeyException("duplicate key", sqlCause));

        // When / Then - exception propagates (no silent swallowing), with original cause preserved
        ProcessDebitCreditNoteUseCase.ProcessingException ex =
            assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
                () -> service.process("intake-123", "<xml>test</xml>",
                                      "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));
        assertInstanceOf(DataIntegrityViolationException.class, ex.getCause());

        // publishFailure is attempted (in real Spring context with ROLLBACK_ONLY
        // transaction this write would be lost, but the call must still be made)
        verify(sagaReplyPort).publishFailure(eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE),
            eq("correlation-123"), anyString());

        // Domain event never published (note not committed)
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void testProcessNoteForSagaRaceConditionResolvesAsSuccess() throws Exception {
        // Given - race condition: idempotency check passes, insert conflicts on
        // uq_processed_debit_credit_notes_source_note_id, and the REQUIRES_NEW re-check
        // finds the document already committed by the concurrent thread → publishSuccess.
        // SQLException with SQLState "23505" is required by isSourceNoteIdViolation().
        SQLException sqlCause = new SQLException(
            "ERROR: duplicate key value violates unique constraint" +
            " \"uq_processed_debit_credit_notes_source_note_id\"", "23505");
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(noteRepository.findBySourceNoteId(anyString()))
            .thenReturn(Optional.empty())          // 1st call: idempotency check — no record yet
            .thenReturn(Optional.of(validNote));     // 2nd call: re-check — concurrent insert committed
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class)))
            .thenThrow(new DuplicateKeyException("duplicate key", sqlCause));

        // When / Then — exception still propagates (prevents Spring UnexpectedRollbackException)
        ProcessDebitCreditNoteUseCase.ProcessingException ex =
            assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
                () -> service.process("intake-123", "<xml>test</xml>",
                                      "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));
        assertInstanceOf(DataIntegrityViolationException.class, ex.getCause());

        // SUCCESS reply published because the document was found on re-check
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123");
        verify(sagaReplyPort, never()).publishFailure(any(), any(), any(), any());

        // Domain event never published by this thread (the winning thread already did so)
        verify(eventPublisher, never()).publish(any());
    }

    /**
     * A plain DataIntegrityViolationException (value-too-long, check-constraint, etc.)
     * is NOT a DuplicateKeyException, so it must:
     *  - skip the race-condition re-check entirely (no second findBySourceNoteId call)
     *  - publish FAILURE with "Constraint violation:" prefix
     *  - increment processFailureCounter, not processRaceConditionResolvedCounter
     *  - throw ProcessingException immediately
     */
    @Test
    void testProcessNoteForSagaNonDuplicateKeyConstraintViolation() throws Exception {
        // Given — data-too-long violation: message has no "duplicate key" or "source_note_id"
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class)))
            .thenThrow(new DataIntegrityViolationException(
                "value too long for type character varying(500)"));

        // When / Then — exception thrown immediately with accurate message
        ProcessDebitCreditNoteUseCase.ProcessingException ex =
            assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
                () -> service.process("intake-123", "<xml>test</xml>",
                                      "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));
        assertInstanceOf(DataIntegrityViolationException.class, ex.getCause());
        assertTrue(ex.getMessage().contains("Constraint violation"),
            "Exception message should say 'Constraint violation', not duplicate-document");

        // FAILURE reply published with "Constraint violation:" prefix — not "Duplicate document:"
        verify(sagaReplyPort).publishFailure(
            eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("correlation-123"),
            contains("Constraint violation"));

        // Re-check MUST NOT happen — transactionManager.getTransaction never called
        verify(transactionManager, never()).getTransaction(any());

        // Domain event never published
        verify(eventPublisher, never()).publish(any());
    }

    /**
     * A DuplicateKeyException whose cause message does NOT contain the
     * source_note_id constraint name (e.g. duplicate note_number from a
     * different document) must be treated as a plain constraint violation:
     *  - no REQUIRES_NEW re-check
     *  - FAILURE reply with "Constraint violation:" prefix
     *  - processFailureCounter incremented, processRaceConditionResolvedCounter NOT
     */
    @Test
    void testProcessNoteForSagaDuplicateKeyOnNonIdempotentConstraint() throws Exception {
        // Given — note_number duplicate (different document, same number): constraint name
        // does NOT contain "uq_processed_debit_credit_notes_source_note_id"
        when(noteRepository.findBySourceNoteId(anyString())).thenReturn(Optional.empty());
        when(parserService.parse(anyString(), anyString())).thenReturn(validNote);
        when(noteRepository.save(any(ProcessedDebitCreditNote.class)))
            .thenThrow(new DuplicateKeyException(
                "duplicate key value violates unique constraint \"idx_note_number_unique\""));

        // When / Then — exception thrown immediately (no re-check)
        ProcessDebitCreditNoteUseCase.ProcessingException ex =
            assertThrows(ProcessDebitCreditNoteUseCase.ProcessingException.class,
                () -> service.process("intake-123", "<xml>test</xml>",
                                      "saga-1", SagaStep.PROCESS_DEBIT_CREDIT_NOTE, "correlation-123"));
        assertInstanceOf(DuplicateKeyException.class, ex.getCause());
        assertTrue(ex.getMessage().contains("Constraint violation"),
            "Exception message should say 'Constraint violation'");

        // FAILURE reply, not success
        verify(sagaReplyPort).publishFailure(
            eq("saga-1"), eq(SagaStep.PROCESS_DEBIT_CREDIT_NOTE), eq("correlation-123"),
            contains("Constraint violation"));
        verify(sagaReplyPort, never()).publishSuccess(any(), any(), any());

        // Re-check MUST NOT happen — only one findBySourceNoteId call (the initial idempotency check)
        verify(noteRepository, times(1)).findBySourceNoteId(anyString());

        // Domain event never published
        verify(eventPublisher, never()).publish(any());
    }
}
