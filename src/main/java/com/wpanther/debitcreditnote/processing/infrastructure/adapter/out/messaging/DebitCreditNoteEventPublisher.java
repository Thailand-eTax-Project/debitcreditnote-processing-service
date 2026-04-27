package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.messaging;

import com.wpanther.debitcreditnote.processing.application.dto.event.DebitCreditNoteProcessedEvent;
import com.wpanther.debitcreditnote.processing.application.port.out.DebitCreditNoteEventPublishingPort;
import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedDomainEvent;
import com.wpanther.debitcreditnote.processing.infrastructure.config.KafkaTopicsProperties;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Debit/Credit Note Event Publisher - driven adapter that publishes events to Kafka via outbox pattern.
 * Implements DebitCreditNoteEventPublishingPort to adhere to hexagonal architecture.
 */
@Component
@Slf4j
public class DebitCreditNoteEventPublisher implements DebitCreditNoteEventPublishingPort {

    private final OutboxService outboxService;
    private final HeaderSerializer headerSerializer;
    private final String debitcreditnoteProcessedTopic;

    /** Production constructor — Spring injects the bound {@link KafkaTopicsProperties}. */
    @Autowired
    public DebitCreditNoteEventPublisher(
            OutboxService outboxService,
            HeaderSerializer headerSerializer,
            KafkaTopicsProperties topics) {
        this(outboxService, headerSerializer, topics.debitcreditnoteProcessed());
    }

    /** Package-private constructor for unit tests that pass the topic string directly. */
    DebitCreditNoteEventPublisher(OutboxService outboxService, HeaderSerializer headerSerializer,
                                   String debitcreditnoteProcessedTopic) {
        this.outboxService = outboxService;
        this.headerSerializer = headerSerializer;
        this.debitcreditnoteProcessedTopic = debitcreditnoteProcessedTopic;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DebitCreditNoteProcessedDomainEvent domainEvent) {
        // Transform: domain event → Kafka event
        DebitCreditNoteProcessedEvent kafkaEvent = new DebitCreditNoteProcessedEvent(
            domainEvent.documentId(),
            domainEvent.documentNumber(),
            domainEvent.total().amount(),
            domainEvent.total().currency(),
            domainEvent.sagaId(),
            domainEvent.correlationId()
        );

        Map<String, String> headers = Map.of(
            "correlationId", domainEvent.correlationId(),
            "documentNumber", domainEvent.documentNumber()
        );

        outboxService.saveWithRouting(
            kafkaEvent,
            "ProcessedDebitCreditNote",
            domainEvent.documentId(),
            debitcreditnoteProcessedTopic,
            domainEvent.documentId(),
            headerSerializer.toJson(headers)
        );

        log.info("Published DebitCreditNoteProcessedEvent to outbox: {}", domainEvent.documentNumber());
    }
}
