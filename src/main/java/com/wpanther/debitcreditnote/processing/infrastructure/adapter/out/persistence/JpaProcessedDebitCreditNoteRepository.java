package com.wpanther.debitcreditnote.processing.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ProcessedDebitCreditNoteEntity.
 * Only contains query methods needed by the trimmed domain port.
 */
public interface JpaProcessedDebitCreditNoteRepository extends JpaRepository<ProcessedDebitCreditNoteEntity, UUID> {

    Optional<ProcessedDebitCreditNoteEntity> findByNoteNumber(String noteNumber);

    Optional<ProcessedDebitCreditNoteEntity> findBySourceNoteId(String sourceNoteId);
}
