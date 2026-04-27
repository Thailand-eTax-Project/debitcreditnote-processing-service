package com.wpanther.debitcreditnote.processing.domain.port.out;

import com.wpanther.debitcreditnote.processing.domain.model.DebitCreditNoteId;
import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;

import java.util.Optional;

public interface ProcessedDebitCreditNoteRepository {

    ProcessedDebitCreditNote save(ProcessedDebitCreditNote note);

    Optional<ProcessedDebitCreditNote> findById(DebitCreditNoteId id);

    Optional<ProcessedDebitCreditNote> findByNoteNumber(String noteNumber);

    Optional<ProcessedDebitCreditNote> findBySourceNoteId(String sourceNoteId);

    void deleteById(DebitCreditNoteId id);
}