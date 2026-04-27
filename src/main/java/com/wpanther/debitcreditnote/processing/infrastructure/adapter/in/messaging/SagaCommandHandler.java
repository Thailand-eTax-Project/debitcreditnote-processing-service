package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging;

import com.wpanther.debitcreditnote.processing.application.port.in.CompensateDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.application.port.in.ProcessDebitCreditNoteUseCase;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.CompensateDebitCreditNoteCommand;
import com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto.ProcessDebitCreditNoteCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Saga command handler - driving adapter that receives Kafka messages and calls use cases.
 * Routes ProcessDebitCreditNoteCommand → ProcessDebitCreditNoteUseCase.process()
 * Routes CompensateDebitCreditNoteCommand → CompensateDebitCreditNoteUseCase.compensate()
 * The use case handles reply publishing internally.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCommandHandler {

    private final ProcessDebitCreditNoteUseCase processUseCase;
    private final CompensateDebitCreditNoteUseCase compensateUseCase;

    /**
     * Handle a ProcessDebitCreditNoteCommand from saga orchestrator.
     * Delegates to use case which handles business logic and reply publishing.
     *
     * <h3>Exception contract</h3>
     * <p>{@link ProcessDebitCreditNoteUseCase.ProcessingException} is thrown by
     * {@code process()} <em>only after</em> the saga reply (SUCCESS or FAILURE) has been
     * successfully committed to the outbox table. Catching it here and returning normally
     * tells Camel "this message is done" — the offset is committed and the orchestrator
     * already has the reply.
     *
     * <p>Any <em>other</em> exception (e.g., a transient DB outage that prevented the
     * outbox write inside {@code publishFailure()} or {@code publishSuccess()}) is
     * intentionally <em>not</em> caught. It propagates to the Camel dead-letter channel,
     * which retries the message. On retry the idempotency check inside
     * {@code process()} either finds an already-persisted note and publishes SUCCESS, or
     * reruns the processing. This ensures the orchestrator always receives a reply and
     * the Kafka offset is only committed once the reply is durable.
     */
    public void handleProcessCommand(ProcessDebitCreditNoteCommand command) {
        log.info("Handling ProcessDebitCreditNoteCommand for saga {} document {}",
            command.getSagaId(), command.getDocumentId());

        try {
            processUseCase.process(
                command.getDocumentId(),
                command.getXmlContent(),
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId()
            );
        } catch (ProcessDebitCreditNoteUseCase.ProcessingException e) {
            // Reply was already committed to the outbox by the use case.
            // Return normally so Camel commits the Kafka offset.
            log.error("Failed to process debit/credit note for saga {}: {}",
                command.getSagaId(), e.toString(), e);
        }
    }

    /**
     * Handle a CompensateDebitCreditNoteCommand from saga orchestrator.
     * Delegates to use case which handles business logic and reply publishing.
     *
     * <h3>Exception contract</h3>
     * <p>On success, {@code compensate()} commits a COMPENSATED reply to the outbox and returns
     * normally; Camel commits the Kafka offset.
     *
     * <p>On failure, {@code compensate()} commits a FAILURE reply to the outbox (via its own
     * REQUIRES_NEW transaction) and then throws {@link CompensateDebitCreditNoteUseCase.CompensationException}.
     * That exception propagates here to Camel's Dead Letter Channel, triggering a retry.
     * Retries are safe because the underlying delete is idempotent when the entity is absent.
     */
    public void handleCompensation(CompensateDebitCreditNoteCommand command) {
        log.info("Handling compensation for saga {} document {}",
            command.getSagaId(), command.getDocumentId());

        compensateUseCase.compensate(
            command.getDocumentId(),
            command.getSagaId(),
            command.getSagaStep(),
            command.getCorrelationId()
        );
    }
}
