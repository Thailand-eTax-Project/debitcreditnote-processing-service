package com.wpanther.debitcreditnote.processing.util;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * JUnit 5 ExecutionCondition that skips tests when Kafka is not available.
 * Tests in classes annotated with {@link KafkaAvailable} will be skipped
 * if Kafka is not reachable at the configured bootstrap servers.
 */
public class KafkaAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String host = System.getProperty("test.kafka.host", "localhost");
        int port = Integer.parseInt(System.getProperty("test.kafka.port", "9093"));
        if (isPortReachable(host, port, 1000)) {
            return ConditionEvaluationResult.enabled("Kafka is available at " + host + ":" + port);
        }
        return ConditionEvaluationResult.disabled(
                "Kafka is not available at " + host + ":" + port + " — skipping test");
    }

    private static boolean isPortReachable(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
