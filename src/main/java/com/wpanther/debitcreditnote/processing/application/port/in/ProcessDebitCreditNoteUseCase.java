package com.wpanther.debitcreditnote.processing.application.port.in;

import com.wpanther.saga.domain.enums.SagaStep;

public interface ProcessDebitCreditNoteUseCase {

    void process(String documentId, String xmlContent,
                 String sagaId, SagaStep sagaStep, String correlationId)
        throws ProcessingException;

    class ProcessingException extends Exception {
        public ProcessingException(String message) {
            super(message);
        }

        public ProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}