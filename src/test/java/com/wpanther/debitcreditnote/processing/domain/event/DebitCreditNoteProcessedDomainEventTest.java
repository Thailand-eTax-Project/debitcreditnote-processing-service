package com.wpanther.debitcreditnote.processing.domain.event;

import com.wpanther.debitcreditnote.processing.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DebitCreditNoteProcessedDomainEventTest {

    @Test
    void of_stampsOccurredAt() {
        Instant before = Instant.now();
        DebitCreditNoteProcessedDomainEvent event = DebitCreditNoteProcessedDomainEvent.of(
            "doc-1", "CN-001", Money.of(BigDecimal.valueOf(100), "THB"), "saga-1", "corr-1"
        );
        Instant after = Instant.now();

        assertThat(event.occurredAt()).isBetween(before, after);
    }

    @Test
    void constructor_setsAllFields() {
        Instant now = Instant.now();
        Money total = Money.of(BigDecimal.valueOf(200), "THB");
        DebitCreditNoteProcessedDomainEvent event = new DebitCreditNoteProcessedDomainEvent(
            "doc-2", "CN-002", total, "saga-2", "corr-2", now
        );

        assertThat(event.documentId()).isEqualTo("doc-2");
        assertThat(event.documentNumber()).isEqualTo("CN-002");
        assertThat(event.total()).isEqualTo(total);
        assertThat(event.sagaId()).isEqualTo("saga-2");
        assertThat(event.correlationId()).isEqualTo("corr-2");
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    void record_equality() {
        Instant now = Instant.now();
        Money total = Money.of(BigDecimal.valueOf(100), "THB");
        DebitCreditNoteProcessedDomainEvent e1 = new DebitCreditNoteProcessedDomainEvent(
            "doc-1", "CN-001", total, "saga-1", "corr-1", now);
        DebitCreditNoteProcessedDomainEvent e2 = new DebitCreditNoteProcessedDomainEvent(
            "doc-1", "CN-001", total, "saga-1", "corr-1", now);
        assertThat(e1).isEqualTo(e2);
    }
}