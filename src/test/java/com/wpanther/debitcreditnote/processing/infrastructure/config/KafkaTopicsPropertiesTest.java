package com.wpanther.debitcreditnote.processing.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KafkaTopicsPropertiesTest {

    @Test
    void recordHoldsAllTopicNames() {
        KafkaTopicsProperties props = new KafkaTopicsProperties(
                "debitcreditnote.processed",
                "debitcreditnote.processing.dlq",
                "saga.command.debitcreditnote",
                "saga.compensation.debitcreditnote",
                "saga.reply.debitcreditnote"
        );

        assertEquals("debitcreditnote.processed", props.debitcreditnoteProcessed());
        assertEquals("debitcreditnote.processing.dlq", props.dlq());
        assertEquals("saga.command.debitcreditnote", props.sagaCommandDebitcreditnote());
        assertEquals("saga.compensation.debitcreditnote", props.sagaCompensationDebitcreditnote());
        assertEquals("saga.reply.debitcreditnote", props.sagaReplyDebitcreditnote());
    }
}