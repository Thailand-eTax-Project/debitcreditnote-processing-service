package com.wpanther.debitcreditnote.processing.infrastructure.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.domain.model.SagaCommand;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Compensation command from the Saga Orchestrator to rollback debit/credit note processing.
 * Consumed from Kafka topic: saga.compensation.debit-credit-note
 */
@Getter
public class CompensateDebitCreditNoteCommand extends SagaCommand {

    private static final long serialVersionUID = 1L;

    @JsonProperty("stepToCompensate")
    private final String stepToCompensate;

    @JsonProperty("documentId")
    private final String documentId;

    @JsonProperty("documentType")
    private final String documentType;

    @JsonCreator
    public CompensateDebitCreditNoteCommand(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("version") int version,
            @JsonProperty("sagaId") String sagaId,
            @JsonProperty("sagaStep") SagaStep sagaStep,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("stepToCompensate") String stepToCompensate,
            @JsonProperty("documentId") String documentId,
            @JsonProperty("documentType") String documentType) {
        super(eventId, occurredAt, eventType, version, sagaId, sagaStep, correlationId);
        this.stepToCompensate = stepToCompensate;
        this.documentId = documentId;
        this.documentType = documentType;
    }

    /**
     * Convenience constructor for testing.
     */
    public CompensateDebitCreditNoteCommand(String sagaId, SagaStep sagaStep, String correlationId,
                                        String stepToCompensate, String documentId, String documentType) {
        super(sagaId, sagaStep, correlationId);
        this.stepToCompensate = stepToCompensate;
        this.documentId = documentId;
        this.documentType = documentType;
    }
}