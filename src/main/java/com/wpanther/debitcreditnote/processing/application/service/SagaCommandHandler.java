package com.wpanther.debitcreditnote.processing.application.service;

import com.wpanther.debitcreditnote.processing.domain.event.CompensateDebitCreditNoteCommand;
import com.wpanther.debitcreditnote.processing.domain.event.ProcessDebitCreditNoteCommand;
import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import com.wpanther.debitcreditnote.processing.domain.repository.ProcessedDebitCreditNoteRepository;
import com.wpanther.debitcreditnote.processing.infrastructure.messaging.SagaReplyPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaCommandHandler {

    private final DebitCreditNoteProcessingService processingService;
    private final SagaReplyPublisher sagaReplyPublisher;
    private final ProcessedDebitCreditNoteRepository noteRepository;

    @Transactional
    public void handleProcessCommand(ProcessDebitCreditNoteCommand command) {
        log.info("Handling ProcessDebitCreditNoteCommand for saga {} document {}",
            command.getSagaId(), command.getDocumentId());

        try {
            processingService.processNoteForSaga(
                command.getDocumentId(),
                command.getXmlContent(),
                command.getCorrelationId()
            );

            sagaReplyPublisher.publishSuccess(
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId()
            );

            log.info("Successfully processed debit/credit note for saga {}", command.getSagaId());

        } catch (Exception e) {
            log.error("Failed to process debit/credit note for saga {}: {}",
                command.getSagaId(), e.getMessage(), e);

            sagaReplyPublisher.publishFailure(
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId(),
                e.getMessage()
            );
        }
    }

    @Transactional
    public void handleCompensation(CompensateDebitCreditNoteCommand command) {
        log.info("Handling compensation for saga {} document {}",
            command.getSagaId(), command.getDocumentId());

        try {
            Optional<ProcessedDebitCreditNote> existing =
                noteRepository.findBySourceNoteId(command.getDocumentId());

            if (existing.isPresent()) {
                noteRepository.deleteById(existing.get().getId());
                log.info("Deleted ProcessedDebitCreditNote {} for compensation",
                    existing.get().getId());
            } else {
                log.info("No ProcessedDebitCreditNote found for document {} - already compensated or never processed",
                    command.getDocumentId());
            }

            sagaReplyPublisher.publishCompensated(
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId()
            );

        } catch (Exception e) {
            log.error("Failed to compensate debit/credit note for saga {}: {}",
                command.getSagaId(), e.getMessage(), e);

            sagaReplyPublisher.publishFailure(
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId(),
                "Compensation failed: " + e.getMessage()
            );
        }
    }
}
