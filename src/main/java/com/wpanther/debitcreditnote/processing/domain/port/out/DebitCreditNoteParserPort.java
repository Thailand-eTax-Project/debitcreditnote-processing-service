package com.wpanther.debitcreditnote.processing.domain.port.out;

import com.wpanther.debitcreditnote.processing.domain.model.ProcessedDebitCreditNote;

public interface DebitCreditNoteParserPort {

    ProcessedDebitCreditNote parse(String xmlContent, String sourceNoteId)
            throws ParsingException;

    class ParsingException extends Exception {
        public ParsingException(String message) {
            super(message);
        }

        public ParsingException(String message, Throwable cause) {
            super(message, cause);
        }

        public static ParsingException forEmpty() {
            return new ParsingException("XML content is null or empty");
        }

        public static ParsingException forOversized(int byteSize, int limitBytes) {
            return new ParsingException(
                "XML payload too large: " + byteSize + " bytes (limit " + limitBytes + " bytes / 500 KB)");
        }

        public static ParsingException forTimeout(long timeoutMs) {
            return new ParsingException(
                "XML parsing timed out after " + timeoutMs + " ms — possible malformed input");
        }

        public static ParsingException forInterrupted() {
            return new ParsingException("XML parsing was interrupted");
        }

        public static ParsingException forUnmarshal(Throwable cause) {
            return new ParsingException("XML parsing failed: " + cause.getMessage(), cause);
        }

        public static ParsingException forUnexpectedRootElement(String className) {
            return new ParsingException("Unexpected root element: " + className);
        }
    }
}