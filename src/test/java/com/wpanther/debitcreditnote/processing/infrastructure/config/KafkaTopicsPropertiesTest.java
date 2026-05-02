package com.wpanther.debitcreditnote.processing.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaTopicsPropertiesTest {

    @Test
    void recordHoldsAllTopicNames() {
        KafkaTopicsProperties props = new KafkaTopicsProperties(
                "debitcreditnote.processed",
                "debitcreditnote.processing.dlq",
                "saga.command.debit-credit-note",
                "saga.compensation.debit-credit-note",
                "saga.reply.debit-credit-note"
        );

        assertEquals("debitcreditnote.processed", props.debitcreditnoteProcessed());
        assertEquals("debitcreditnote.processing.dlq", props.dlq());
        assertEquals("saga.command.debit-credit-note", props.sagaCommandDebitCreditNote());
        assertEquals("saga.compensation.debit-credit-note", props.sagaCompensationDebitCreditNote());
        assertEquals("saga.reply.debit-credit-note", props.sagaReplyDebitCreditNote());
    }
}