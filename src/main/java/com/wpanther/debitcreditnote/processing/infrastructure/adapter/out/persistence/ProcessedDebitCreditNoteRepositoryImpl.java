package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import com.wpanther.debitcreditnote.processing.domain.port.out.ProcessedDebitCreditNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of ProcessedDebitCreditNoteRepository using Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProcessedDebitCreditNoteRepositoryImpl implements ProcessedDebitCreditNoteRepository {

    private final JpaProcessedDebitCreditNoteRepository entityRepository;
    private final ProcessedDebitCreditNoteMapper mapper;

    @Override
    @Transactional
    public ProcessedDebitCreditNote save(ProcessedDebitCreditNote note) {
        log.debug("Saving processed debit/credit note: {}", note.getNoteNumber());
        ProcessedDebitCreditNoteEntity saved = entityRepository.save(mapper.toEntity(note));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedDebitCreditNote> findById(DebitCreditNoteId id) {
        return entityRepository.findById(id.getValue())
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedDebitCreditNote> findByNoteNumber(String noteNumber) {
        return entityRepository.findByNoteNumber(noteNumber)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedDebitCreditNote> findBySourceNoteId(String sourceNoteId) {
        return entityRepository.findBySourceNoteId(sourceNoteId)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(DebitCreditNoteId id) {
        log.info("Deleting debit/credit note with ID: {}", id);
        entityRepository.deleteById(id.getValue());
    }
}
