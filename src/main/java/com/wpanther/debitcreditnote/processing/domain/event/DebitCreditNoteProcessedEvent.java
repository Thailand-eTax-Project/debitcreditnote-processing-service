package com.wpanther.debitcreditnote.processing.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wpanther.saga.domain.model.IntegrationEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class DebitCreditNoteProcessedEvent extends IntegrationEvent {

    private static final long serialVersionUID = 1L;

    @JsonProperty("noteId")
    private final UUID noteId;

    @JsonProperty("noteNumber")
    private final String noteNumber;

    @JsonProperty("total")
    private final BigDecimal total;

    @JsonProperty("currency")
    private final String currency;

    @JsonProperty("correlationId")
    private final String correlationId;

    public DebitCreditNoteProcessedEvent(UUID noteId, String noteNumber, BigDecimal total,
                                       String currency, String correlationId) {
        super();
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.total = total;
        this.currency = currency;
        this.correlationId = correlationId;
    }

    public DebitCreditNoteProcessedEvent(UUID eventId, Instant occurredAt, String eventType, int version,
                                         UUID noteId, String noteNumber, BigDecimal total,
                                         String currency, String correlationId) {
        super(eventId, occurredAt, eventType, version);
        this.noteId = noteId;
        this.noteNumber = noteNumber;
        this.total = total;
        this.currency = currency;
        this.correlationId = correlationId;
    }
}

