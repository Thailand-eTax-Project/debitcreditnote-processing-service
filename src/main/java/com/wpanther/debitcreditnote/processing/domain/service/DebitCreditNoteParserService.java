package com.wpanther.debitcreditnote.processing.domain.service;

import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;

public interface DebitCreditNoteParserService {

    ProcessedDebitCreditNote parseNote(String xmlContent, String sourceNoteId)
            throws DebitCreditNoteParsingException;

    class DebitCreditNoteParsingException extends Exception {
        public DebitCreditNoteParsingException(String message) {
            super(message);
        }

        public DebitCreditNoteParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
