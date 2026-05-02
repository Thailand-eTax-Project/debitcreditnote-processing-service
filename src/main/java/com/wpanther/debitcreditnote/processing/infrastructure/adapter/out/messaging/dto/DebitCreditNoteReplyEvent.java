package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging.dto;

import com.wpanther.saga.domain.enums.ReplyStatus;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.domain.model.SagaReply;

/**
 * Concrete SagaReply for the debit-credit-note-processing-service.
 * Published to Kafka topic: saga.reply.debit-credit-note
 */
public class DebitCreditNoteReplyEvent extends SagaReply {

    private static final long serialVersionUID = 1L;

    /**
     * Create a SUCCESS reply.
     */
    public static DebitCreditNoteReplyEvent success(String sagaId, SagaStep sagaStep, String correlationId) {
        return new DebitCreditNoteReplyEvent(sagaId, sagaStep, correlationId, ReplyStatus.SUCCESS);
    }

    /**
     * Create a FAILURE reply.
     */
    public static DebitCreditNoteReplyEvent failure(String sagaId, SagaStep sagaStep, String correlationId,
                                                String errorMessage) {
        return new DebitCreditNoteReplyEvent(sagaId, sagaStep, correlationId, errorMessage);
    }

    /**
     * Create a COMPENSATED reply.
     */
    public static DebitCreditNoteReplyEvent compensated(String sagaId, SagaStep sagaStep, String correlationId) {
        return new DebitCreditNoteReplyEvent(sagaId, sagaStep, correlationId, ReplyStatus.COMPENSATED);
    }

    // For SUCCESS and COMPENSATED (delegates to SagaReply 4-arg status constructor)
    private DebitCreditNoteReplyEvent(String sagaId, SagaStep sagaStep, String correlationId, ReplyStatus status) {
        super(sagaId, sagaStep, correlationId, status);
    }

    // For FAILURE (delegates to SagaReply 4-arg error constructor)
    private DebitCreditNoteReplyEvent(String sagaId, SagaStep sagaStep, String correlationId, String errorMessage) {
        super(sagaId, sagaStep, correlationId, errorMessage);
    }
}