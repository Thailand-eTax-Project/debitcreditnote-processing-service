package com.wpanther.debitcreditnote.processing.application.port.in;

import com.wpanther.saga.domain.enums.SagaStep;

public interface CompensateDebitCreditNoteUseCase {

    void compensate(String documentId, String sagaId,
                    SagaStep sagaStep, String correlationId);

    class CompensationException extends RuntimeException {
        public CompensationException(String message) {
            super(message);
        }

        public CompensationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}