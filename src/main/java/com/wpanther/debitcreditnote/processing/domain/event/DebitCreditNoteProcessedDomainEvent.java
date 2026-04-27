package com.wpanther.debitcreditnote.processing.domain.event;

import com.wpanther.debitcreditnote.processing.domain.model.Money;

import java.time.Instant;

public record DebitCreditNoteProcessedDomainEvent(
    String documentId,
    String documentNumber,
    Money total,
    String sagaId,
    String correlationId,
    Instant occurredAt
) {
    public static DebitCreditNoteProcessedDomainEvent of(
            String documentId, String documentNumber, Money total,
            String sagaId, String correlationId) {
        return new DebitCreditNoteProcessedDomainEvent(
            documentId, documentNumber, total, sagaId, correlationId, Instant.now());
    }
}