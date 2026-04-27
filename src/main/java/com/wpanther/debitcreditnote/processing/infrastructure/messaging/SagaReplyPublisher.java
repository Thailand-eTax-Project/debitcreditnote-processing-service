package com.wpanther.debitcreditnote.processing.infrastructure.messaging;

import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteReplyEvent;
import com.wpanther.saga.domain.enums.ReplyStatus;
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
public class SagaReplyPublisher {

    private final OutboxService outboxService;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishSuccess(String sagaId, String sagaStep, String correlationId) {
        DebitCreditNoteReplyEvent reply = new DebitCreditNoteReplyEvent(sagaId, sagaStep, correlationId);
        publishReply(reply, "SUCCESS");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishFailure(String sagaId, String sagaStep, String correlationId, String errorMessage) {
        DebitCreditNoteReplyEvent reply = new DebitCreditNoteReplyEvent(sagaId, sagaStep, correlationId, errorMessage);
        publishReply(reply, "FAILURE");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishCompensated(String sagaId, String sagaStep, String correlationId) {
        DebitCreditNoteReplyEvent reply = new DebitCreditNoteReplyEvent(sagaId, sagaStep, correlationId, ReplyStatus.COMPENSATED);
        publishReply(reply, "COMPENSATED");
    }

    private void publishReply(DebitCreditNoteReplyEvent reply, String status) {
        try {
            Map<String, Object> headers = Map.of(
                "eventType", "DebitCreditNoteReplyEvent",
                "status", status,
                "sagaId", reply.getSagaId(),
                "sagaStep", reply.getSagaStep(),
                "correlationId", reply.getCorrelationId()
            );

            outboxService.saveWithRouting(
                    reply,
                    "ProcessedDebitCreditNote",
                    reply.getSagaId(),
                    "saga.reply.debitcreditnote",
                    reply.getSagaId(),
                    null
            );

            log.info("Published {} reply for saga: {}, step: {}",
                status, reply.getSagaId(), reply.getSagaStep());

        } catch (Exception e) {
            log.error("Failed to publish saga reply to outbox", e);
            throw new RuntimeException("Failed to publish saga reply to outbox", e);
        }
    }
}
