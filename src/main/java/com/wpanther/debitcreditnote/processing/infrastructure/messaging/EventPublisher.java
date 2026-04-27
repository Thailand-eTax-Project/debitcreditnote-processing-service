package com.wpanther.debitcreditnote.processing.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedEvent;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishDebitCreditNoteProcessed(DebitCreditNoteProcessedEvent event) {
        try {
            String aggregateId = event.getNoteId() != null ? event.getNoteId().toString() : "unknown";

            Map<String, Object> headers = Map.of(
                "eventType", "DebitCreditNoteProcessedEvent",
                "noteNumber", event.getNoteNumber(),
                "correlationId", event.getCorrelationId()
            );

            outboxService.saveWithRouting(
                    event,
                    "ProcessedDebitCreditNote",
                    aggregateId,
                    "debitcreditnote.processed",
                    aggregateId,
                    objectMapper.writeValueAsString(headers)
            );

            log.info("Published DebitCreditNoteProcessedEvent to outbox for note: {}", event.getNoteNumber());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize headers for DebitCreditNoteProcessedEvent", e);
            throw new RuntimeException("Failed to serialize headers for event", e);
        } catch (Exception e) {
            log.error("Failed to publish DebitCreditNoteProcessedEvent to outbox", e);
            throw new RuntimeException("Failed to publish event to outbox", e);
        }
    }
}
