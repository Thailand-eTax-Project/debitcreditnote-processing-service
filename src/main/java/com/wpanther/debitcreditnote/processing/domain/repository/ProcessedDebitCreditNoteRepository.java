package com.wpanther.debitcreditnote.processing.domain.repository;

import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessingStatus;

import java.util.List;
import java.util.Optional;

public interface ProcessedDebitCreditNoteRepository {

    ProcessedDebitCreditNote save(ProcessedDebitCreditNote note);

    Optional<ProcessedDebitCreditNote> findById(DebitCreditNoteId id);

    Optional<ProcessedDebitCreditNote> findByIdWithDetails(DebitCreditNoteId id);

    List<ProcessedDebitCreditNote> findByStatusWithDetails(ProcessingStatus status);

    Optional<ProcessedDebitCreditNote> findByNoteNumber(String noteNumber);

    Optional<ProcessedDebitCreditNote> findBySourceNoteId(String sourceNoteId);

    void deleteById(DebitCreditNoteId id);

    boolean existsBySourceNoteId(String sourceNoteId);
}
