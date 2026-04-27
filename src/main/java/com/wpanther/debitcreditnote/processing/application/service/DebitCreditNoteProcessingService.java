package com.wpanther.debitcreditnote.processing.application.service;

import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.debitcreditnote.processing.application.port.in.CompensateDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.out.DebitCreditNoteEventPublishingPort;
import com.wpanther.debitcreditnote.processing.application.port.out.SagaReplyPort;
import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedDomainEvent;
import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessingStatus;
import com.wpanther.debitcreditnote.processing.domain.port.out.DebitCreditNoteParserPort;
import com.wpanther.debitcreditnote.processing.domain.port.out.ProcessedDebitCreditNoteRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Application service for debit/credit note processing.
 * Implements inbound ports for processing and compensation.
 * Uses outbound ports via DebitCreditNoteEventPublishingPort for event publishing
 * and SagaReplyPort for saga replies.
 */
@Service
@Slf4j
public class DebitCreditNoteProcessingService
        implements ProcessDebitCreditNoteUseCase, CompensateDebitCreditNoteUseCase {

    private final ProcessedDebitCreditNoteRepository noteRepository;
    private final DebitCreditNoteParserPort parserService;
    private final DebitCreditNoteEventPublishingPort eventPublisher;
    private final SagaReplyPort sagaReplyPort;
    private final MeterRegistry meterRegistry;

    // Fresh-transaction executor for replying after a ROLLBACK_ONLY outer transaction
    private final TransactionTemplate requiresNewTemplate;

    // Metrics - initialized once in constructor
    private final Counter processSuccessCounter;
    private final Counter processFailureCounter;
    private final Counter processIdempotentCounter;
    private final Counter processRaceConditionResolvedCounter;
    private final Counter compensateSuccessCounter;
    private final Counter compensateIdempotentCounter;
    private final Counter compensateFailureCounter;
    private final Timer processingTimer;

    public DebitCreditNoteProcessingService(
            ProcessedDebitCreditNoteRepository noteRepository,
            DebitCreditNoteParserPort parserService,
            DebitCreditNoteEventPublishingPort eventPublisher,
            SagaReplyPort sagaReplyPort,
            MeterRegistry meterRegistry,
            PlatformTransactionManager transactionManager) {
        this.noteRepository = noteRepository;
        this.parserService = parserService;
        this.eventPublisher = eventPublisher;
        this.sagaReplyPort = sagaReplyPort;
        this.meterRegistry = meterRegistry;

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.requiresNewTemplate = template;

        // Initialize metrics once
        this.processSuccessCounter = Counter.builder("debitcreditnote.processing.success")
            .description("Number of successfully processed debit/credit notes")
            .register(meterRegistry);
        this.processFailureCounter = Counter.builder("debitcreditnote.processing.failure")
            .description("Number of failed debit/credit note processing attempts")
            .register(meterRegistry);
        this.processIdempotentCounter = Counter.builder("debitcreditnote.processing.idempotent")
            .description("Number of duplicate processing requests handled idempotently")
            .register(meterRegistry);
        this.processRaceConditionResolvedCounter = Counter.builder("debitcreditnote.processing.race_condition_resolved")
            .description("Number of DuplicateKeyExceptions on source_note_id resolved as concurrent inserts — re-check confirmed the document was committed by another thread")
            .register(meterRegistry);
        this.compensateSuccessCounter = Counter.builder("debitcreditnote.compensation.success")
            .description("Number of successful compensations")
            .register(meterRegistry);
        this.compensateIdempotentCounter = Counter.builder("debitcreditnote.compensation.idempotent")
            .description("Number of duplicate compensation commands received for an already-deleted note")
            .register(meterRegistry);
        this.compensateFailureCounter = Counter.builder("debitcreditnote.compensation.failure")
            .description("Number of failed compensation attempts")
            .register(meterRegistry);
        this.processingTimer = Timer.builder("debitcreditnote.processing.duration")
            .description("Time taken to process debit/credit notes")
            .register(meterRegistry);
    }

    /**
     * Process debit/credit note as part of a saga command.
     * Parses XML, validates, calculates totals, saves to DB, publishes notification event.
     */
    @Override
    @Transactional
    public void process(String documentId, String xmlContent,
                         String sagaId, SagaStep sagaStep, String correlationId) throws ProcessingException {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            processNoteInternal(documentId, xmlContent, sagaId, sagaStep, correlationId);
        } catch (DebitCreditNoteParserPort.ParsingException e) {
            processFailureCounter.increment();
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId, "Parse error: " + e.toString());
            throw new ProcessingException("Failed to parse debit/credit note: " + e.toString(), e);
        } catch (DuplicateKeyException e) {
            // Only the source_note_id constraint violation indicates a potential race condition
            // (two threads inserting the same document concurrently). Any other unique constraint
            // violation (e.g. duplicate note_number from a different document) is a data error
            // and must fail immediately without a REQUIRES_NEW re-check.
            if (!isSourceNoteIdViolation(e)) {
                processFailureCounter.increment();
                log.error("Duplicate key violation on non-idempotent constraint for document {}, saga {}: {}",
                        documentId, sagaId, e.toString());
                sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                        "Constraint violation for document " + documentId + ": " + e.toString());
                throw new ProcessingException(
                        "Constraint violation for document " + documentId, e);
            }

            // Race-condition duplicate insert on source_note_id unique constraint.
            // Spring translates both PostgreSQL error 23505 and H2 unique index violations
            // to DuplicateKeyException. isSourceNoteIdViolation() additionally gates
            // on SQLState "23505" (ANSI unique_violation) and the constraint name to
            // distinguish this path from other unique violations.
            // The outer transaction is ROLLBACK_ONLY; re-check in a fresh REQUIRES_NEW
            // transaction so we can reply SUCCESS if a concurrent thread already committed
            // the document — preventing the orchestrator from compensating committed work.
            log.warn("DuplicateKeyException on source_note_id for document {}, saga {} — re-checking for concurrent insert",
                    documentId, sagaId);
            requiresNewTemplate.execute(txStatus -> {
                Optional<ProcessedDebitCreditNote> existing = noteRepository.findBySourceNoteId(documentId);
                if (existing.isPresent()) {
                    // Concurrent thread committed the same document first; treat as idempotent success.
                    log.warn("Race condition resolved: document {} already committed by concurrent thread — replying SUCCESS",
                            documentId);
                    processRaceConditionResolvedCounter.increment();
                    sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);
                } else {
                    // source_note_id constraint fired but no record found — unexpected state.
                    log.error("DuplicateKeyException on source_note_id for document {} but no record found — replying FAILURE",
                            documentId);
                    processFailureCounter.increment();
                    sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                            "Duplicate key violation for document " + documentId + ": " + e.toString());
                }
                return null;
            });
            // Always throw so Spring does not try to commit the ROLLBACK_ONLY outer
            // transaction (which would raise UnexpectedRollbackException past SagaCommandHandler).
            throw new ProcessingException("Concurrent insert for document: " + documentId, e);
        } catch (DataIntegrityViolationException e) {
            // Other constraint violations (value-too-long, check-constraint, etc.).
            // These are not race-condition duplicates and must not be treated as idempotent.
            processFailureCounter.increment();
            log.error("Constraint violation (non-duplicate-key) for document {}, saga {}: {}",
                    documentId, sagaId, e.toString());
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                    "Constraint violation for document " + documentId + ": " + e.toString());
            throw new ProcessingException(
                    "Constraint violation for document " + documentId, e);
        } catch (Exception e) {
            processFailureCounter.increment();
            // publishFailure uses REQUIRES_NEW propagation — commits in its own independent
            // transaction even if the outer transaction is ROLLBACK_ONLY or the Hibernate
            // session is invalid.
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                    "Processing error for document " + documentId + ": " + e.toString());
            throw new ProcessingException(
                    "Failed to process debit/credit note " + documentId + ": " + e.toString(), e);
        } finally {
            sample.stop(processingTimer);
        }
    }

    private ProcessedDebitCreditNote processNoteInternal(String documentId, String xmlContent,
                                                          String sagaId, SagaStep sagaStep, String correlationId)
            throws DebitCreditNoteParserPort.ParsingException {
        log.info("Processing debit/credit note for saga, document: {}", documentId);

        // Idempotency check — also resumes partial-failure where a previous attempt
        // saved the entity in PROCESSING state but died before reaching COMPLETED.
        Optional<ProcessedDebitCreditNote> existing = noteRepository.findBySourceNoteId(documentId);
        if (existing.isPresent()) {
            ProcessedDebitCreditNote existingNote = existing.get();

            if (existingNote.getStatus() == ProcessingStatus.COMPLETED) {
                // True idempotent case: a prior attempt fully committed this document.
                log.warn("Debit/credit note already completed for document {}, returning idempotent success", documentId);
                processIdempotentCounter.increment();
                sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);
                return existingNote;
            }

            if (existingNote.getStatus() == ProcessingStatus.PROCESSING) {
                // Partial failure: entity was inserted (PROCESSING) but the attempt died
                // before markCompleted() + second save could commit. Resume from here
                // without re-parsing or re-inserting — avoids a duplicate-key violation
                // and ensures the orchestrator always receives a SUCCESS reply.
                log.warn("Debit/credit note for document {} found in PROCESSING state — previous attempt "
                        + "failed mid-flight; resuming completion", documentId);
                existingNote.markCompleted();
                noteRepository.save(existingNote);
                DebitCreditNoteProcessedDomainEvent domainEvent = DebitCreditNoteProcessedDomainEvent.of(
                    existingNote.getSourceNoteId(),
                    existingNote.getNoteNumber(),
                    existingNote.getTotal(),
                    sagaId,
                    correlationId
                );
                eventPublisher.publish(domainEvent);
                sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);
                processSuccessCounter.increment();
                log.info("Resumed and completed debit/credit note: {}", existingNote.getNoteNumber());
                return existingNote;
            }

            // PENDING is never persisted; FAILED not yet implemented.
            // Surface unexpected state as a clear error rather than silently mis-routing.
            throw new IllegalStateException(
                "Debit/credit note for document " + documentId + " has unexpected persisted status: "
                    + existingNote.getStatus());
        }

        // Parse XML to domain model
        ProcessedDebitCreditNote note = parserService.parse(xmlContent, documentId);

        // State: PENDING → PROCESSING
        note.startProcessing();

        // Save - direct call (race conditions handled by Camel retry if they occur)
        ProcessedDebitCreditNote saved = noteRepository.save(note);

        log.info("Saved processed debit/credit note: {}", saved.getNoteNumber());

        // State: PROCESSING → COMPLETED
        saved.markCompleted();
        noteRepository.save(saved);

        // Publish notification event (kept for notification-service)
        DebitCreditNoteProcessedDomainEvent domainEvent = DebitCreditNoteProcessedDomainEvent.of(
            saved.getSourceNoteId(),
            saved.getNoteNumber(),
            saved.getTotal(),
            sagaId,
            correlationId
        );
        eventPublisher.publish(domainEvent);

        // Publish saga success reply
        sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);

        processSuccessCounter.increment();
        log.info("Successfully processed debit/credit note: {}", saved.getNoteNumber());
        return saved;
    }

    /**
     * Compensate/rollback a previously processed debit/credit note.
     */
    @Override
    @Transactional
    public void compensate(String documentId, String sagaId, SagaStep sagaStep, String correlationId) {
        log.info("Compensating debit/credit note for document: {}", documentId);

        try {
            Optional<ProcessedDebitCreditNote> existing = noteRepository.findBySourceNoteId(documentId);
            if (existing.isPresent()) {
                noteRepository.deleteById(existing.get().getId());
                log.info("Deleted debit/credit note for document: {}", documentId);
            } else {
                // Note already absent — duplicate compensation command (orchestrator retry or bug).
                compensateIdempotentCounter.increment();
                log.warn("Debit/credit note not found for compensation of document {} saga {} — "
                    + "treating as idempotent duplicate (already compensated or never processed)",
                    documentId, sagaId);
            }

            sagaReplyPort.publishCompensated(sagaId, sagaStep, correlationId);
            compensateSuccessCounter.increment();
        } catch (Exception e) {
            compensateFailureCounter.increment();
            log.error("Failed to compensate debit/credit note for saga {}: {}", sagaId, e.toString(), e);
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId, "Compensation failed: " + e.toString());
            // Rethrow so Camel receives a clean exception and triggers DLC retry,
            // rather than Spring throwing UnexpectedRollbackException when it tries
            // to commit the ROLLBACK_ONLY outer transaction after a silent return.
            // deleteById is idempotent (no-op if entity is absent), so retries are safe.
            throw new CompensationException(
                    "Compensation failed for document " + documentId, e);
        }
    }

    /**
     * Returns {@code true} only when the exception is specifically a unique_violation
     * on the {@code uq_processed_debit_credit_notes_source_note_id} constraint — the
     * sole case that indicates a concurrent insert of the same document rather than
     * a genuine data error.
     *
     * <p>Detection strategy (two independent guards, both must match):
     * <ol>
     *   <li><b>SQLState "23505"</b> — the ANSI / PostgreSQL / H2 code for
     *       {@code unique_violation}.  This is stable across DB versions and
     *       drivers and filters out unrelated {@code DataIntegrityViolationException}
     *       subclasses that Spring may also wrap as {@code DuplicateKeyException}.</li>
     *   <li><b>Constraint name in the message</b> — narrows the match to
     *       <em>this specific</em> constraint so that a duplicate
     *       {@code note_number} (a different unique index) is not treated as an
     *       idempotent race condition.  The constraint name is set by Flyway migration
     *       V1 and must stay in sync with the value below if ever renamed.</li>
     * </ol>
     *
     * <p><b>Dialect note</b>: PostgreSQL formats the constraint name inside the
     * detail message ("Key (source_note_id)=(...) already exists"); H2 formats it
     * differently but still includes the index name.  Both dialects emit SQLState
     * "23505" for unique violations.  If the constraint is renamed in a future
     * migration this method must be updated to match.
     */
    private static boolean isSourceNoteIdViolation(DuplicateKeyException e) {
        Throwable cause = e.getMostSpecificCause();
        String msg = cause.getMessage();
        if (msg == null || !msg.contains("uq_processed_debit_credit_notes_source_note_id")) {
            return false;
        }
        // Walk the cause chain for a SQLException carrying SQLState "23505".
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sqlEx && "23505".equals(sqlEx.getSQLState())) {
                return true;
            }
        }
        // No SQLException with SQLState 23505 found — treat as non-race-condition violation.
        return false;
    }

}
