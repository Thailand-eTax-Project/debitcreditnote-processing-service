package com.wpanther.debitcreditnote.processing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for all {@code app.kafka.topics.*} configuration properties.
 *
 * <p>Centralises the property keys in one place so that a mistyped key fails at
 * startup (Spring Boot throws {@code BindException} for unresolvable fields) rather
 * than silently routing events to the wrong topic at runtime.
 *
 * <p>YAML key mapping (Spring relaxed binding):
 * <pre>
 *   app.kafka.topics.debitcreditnote-processed          → debitcreditnoteProcessed
 *   app.kafka.topics.dlq                               → dlq
 *   app.kafka.topics.saga-command-debitcreditnote      → sagaCommandDebitcreditnote
 *   app.kafka.topics.saga-compensation-debitcreditnote → sagaCompensationDebitcreditnote
 *   app.kafka.topics.saga-reply-debitcreditnote        → sagaReplyDebitcreditnote
 * </pre>
 */
@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(
        String debitcreditnoteProcessed,
        String dlq,
        String sagaCommandDebitcreditnote,
        String sagaCompensationDebitcreditnote,
        String sagaReplyDebitcreditnote) {
}