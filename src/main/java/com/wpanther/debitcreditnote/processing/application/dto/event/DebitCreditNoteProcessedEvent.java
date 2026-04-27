package com.wpanther.debitcreditnote.processing.application.dto.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wpanther.saga.domain.model.TraceEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when debit/credit note processing is completed.
 * This is a trace event for audit/notification purposes.
 * Published to Kafka topic: debitcreditnote.processed
 */
@Getter
public class DebitCreditNoteProcessedEvent extends TraceEvent {

    private static final String EVENT_TYPE = "debitcreditnote.processed";
    private static final String SOURCE = "debitcreditnote-processing-service";
    private static final String TRACE_TYPE = "DEBITCREDITNOTE_PROCESSED";

    @JsonProperty("documentId")
    private final String documentId;

    @JsonProperty("documentNumber")
    private final String documentNumber;

    @JsonProperty("amount")
    private final BigDecimal amount;

    @JsonProperty("currency")
    private final String currency;

    /**
     * Convenience constructor for creating the event.
     *
     * @param documentId     the processed document ID
     * @param documentNumber the document number
     * @param amount         the debit/credit note amount
     * @param currency       the currency code
     * @param sagaId         the saga orchestration instance ID
     * @param correlationId  the end-to-end correlation ID from the originating request
     */
    public DebitCreditNoteProcessedEvent(String documentId, String documentNumber, BigDecimal amount,
                                        String currency, String sagaId, String correlationId) {
        super(sagaId, correlationId, SOURCE, TRACE_TYPE, null);
        this.documentId = documentId;
        this.documentNumber = documentNumber;
        this.amount = amount;
        this.currency = currency;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @JsonCreator
    public DebitCreditNoteProcessedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("occurredAt") Instant occurredAt,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("version") int version,
        @JsonProperty("sagaId") String sagaId,
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("source") String source,
        @JsonProperty("traceType") String traceType,
        @JsonProperty("context") String context,
        @JsonProperty("documentId") String documentId,
        @JsonProperty("documentNumber") String documentNumber,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency
    ) {
        super(eventId, occurredAt, eventType, version, sagaId, correlationId, source, traceType, context);
        this.documentId = documentId;
        this.documentNumber = documentNumber;
        this.amount = amount;
        this.currency = currency;
    }
}