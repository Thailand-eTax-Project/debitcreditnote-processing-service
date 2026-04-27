package com.wpanther.debitcreditnote.processing.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wpanther.saga.domain.model.SagaReply;
import com.wpanther.saga.domain.enums.ReplyStatus;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class DebitCreditNoteReplyEvent extends SagaReply {

    private static final long serialVersionUID = 1L;

    public DebitCreditNoteReplyEvent(String sagaId, String sagaStep, String correlationId) {
        super(sagaId, sagaStep, correlationId);
    }

    public DebitCreditNoteReplyEvent(String sagaId, String sagaStep, String correlationId, String errorMessage) {
        super(sagaId, sagaStep, correlationId, errorMessage);
    }

    public DebitCreditNoteReplyEvent(String sagaId, String sagaStep, String correlationId, ReplyStatus status) {
        super(sagaId, sagaStep, correlationId, status);
    }

    @JsonCreator
    public DebitCreditNoteReplyEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("version") int version,
            @JsonProperty("sagaId") String sagaId,
            @JsonProperty("sagaStep") String sagaStep,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("status") ReplyStatus status,
            @JsonProperty("errorMessage") String errorMessage) {
        super(eventId, occurredAt, eventType, version, sagaId, sagaStep, correlationId, status, errorMessage);
    }
}
