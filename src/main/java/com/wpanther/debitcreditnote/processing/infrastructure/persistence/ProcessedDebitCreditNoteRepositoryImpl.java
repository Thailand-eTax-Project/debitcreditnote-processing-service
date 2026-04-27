package com.wpanther.debitcreditnote.processing.infrastructure.persistence;

import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessingStatus;
import com.wpanther.debitcreditnote.processing.domain.repository.ProcessedDebitCreditNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProcessedDebitCreditNoteRepositoryImpl implements ProcessedDebitCreditNoteRepository {

    private final JpaProcessedDebitCreditNoteRepository jpaRepository;
    private final ProcessedDebitCreditNoteMapper mapper;

    @Override
    public ProcessedDebitCreditNote save(ProcessedDebitCreditNote note) {
        ProcessedDebitCreditNoteEntity entity = mapper.toEntity(note);
        ProcessedDebitCreditNoteEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProcessedDebitCreditNote> findById(DebitCreditNoteId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ProcessedDebitCreditNote> findByIdWithDetails(DebitCreditNoteId id) {
        return jpaRepository.findByIdWithDetails(id.getValue())
                .map(mapper::toDomain);
    }

    @Override
    public List<ProcessedDebitCreditNote> findByStatusWithDetails(ProcessingStatus status) {
        return jpaRepository.findByStatusWithDetails(status.toString())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProcessedDebitCreditNote> findByNoteNumber(String noteNumber) {
        return jpaRepository.findByNoteNumber(noteNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ProcessedDebitCreditNote> findBySourceNoteId(String sourceNoteId) {
        return jpaRepository.findBySourceNoteId(sourceNoteId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(DebitCreditNoteId id) {
        jpaRepository.deleteById(id.getValue());
    }

    @Override
    public boolean existsBySourceNoteId(String sourceNoteId) {
        return jpaRepository.findBySourceNoteId(sourceNoteId).isPresent();
    }
}
