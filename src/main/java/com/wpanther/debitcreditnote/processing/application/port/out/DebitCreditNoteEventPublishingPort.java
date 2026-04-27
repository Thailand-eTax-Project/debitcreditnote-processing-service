package com.wpanther.debitcreditnote.processing.application.port.out;

import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedDomainEvent;

public interface DebitCreditNoteEventPublishingPort {

    void publish(DebitCreditNoteProcessedDomainEvent event);
}