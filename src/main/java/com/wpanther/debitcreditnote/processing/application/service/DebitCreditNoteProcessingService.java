package com.wpanther.debitcreditnote.processing.application.service;

import com.wpanther.debitcreditnote.processing.domain.event.DebitCreditNoteProcessedEvent;
import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import com.wpanther.debitcreditnote.processing.domain.repository.ProcessedDebitCreditNoteRepository;
import com.wpanther.debitcreditnote.processing.domain.service.DebitCreditNoteParserService;
import com.wpanther.debitcreditnote.processing.domain.service.DebitCreditNoteParserService.DebitCreditNoteParsingException;
import com.wpanther.debitcreditnote.processing.infrastructure.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebitCreditNoteProcessingService {

    private final DebitCreditNoteParserService parserService;
    private final ProcessedDebitCreditNoteRepository noteRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void processNoteForSaga(String sourceNoteId, String xmlContent, String correlationId) throws DebitCreditNoteParsingException {
        log.info("Processing debit/credit note for source ID: {}", sourceNoteId);

        if (noteRepository.existsBySourceNoteId(sourceNoteId)) {
            log.info("Note with source ID {} already processed, skipping", sourceNoteId);
            return;
        }

        ProcessedDebitCreditNote note = parserService.parseNote(xmlContent, sourceNoteId);
        note.startProcessing();
        ProcessedDebitCreditNote saved = noteRepository.save(note);
        saved.markCompleted();
        noteRepository.save(saved);

        eventPublisher.publishDebitCreditNoteProcessed(
            new DebitCreditNoteProcessedEvent(
                saved.getId().getValue(),
                saved.getNoteNumber(),
                saved.getTotal().amount(),
                saved.getCurrency(),
                correlationId
            )
        );

        log.info("Successfully processed debit/credit note: {}", saved.getNoteNumber());
    }

    public Optional<ProcessedDebitCreditNote> findNoteById(DebitCreditNoteId id) {
        return noteRepository.findById(id);
    }
}
