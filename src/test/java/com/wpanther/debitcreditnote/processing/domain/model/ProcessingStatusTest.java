package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProcessingStatus enum
 */
class ProcessingStatusTest {

    @Test
    void hasAllExpectedValues() {
        // When
        ProcessingStatus[] statuses = ProcessingStatus.values();

        // Then
        assertEquals(4, statuses.length);
        assertArrayEquals(
            new ProcessingStatus[]{
                ProcessingStatus.PENDING,
                ProcessingStatus.PROCESSING,
                ProcessingStatus.COMPLETED,
                ProcessingStatus.FAILED
            },
            statuses
        );
    }

    @Test
    void valueOf() {
        // When/Then
        assertEquals(ProcessingStatus.PENDING, ProcessingStatus.valueOf("PENDING"));
        assertEquals(ProcessingStatus.PROCESSING, ProcessingStatus.valueOf("PROCESSING"));
        assertEquals(ProcessingStatus.COMPLETED, ProcessingStatus.valueOf("COMPLETED"));
        assertEquals(ProcessingStatus.FAILED, ProcessingStatus.valueOf("FAILED"));
    }

    @Test
    void valueOf_invalid_throws() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            ProcessingStatus.valueOf("INVALID_STATUS")
        );
    }

    @Test
    void enumEquality() {
        // When/Then
        assertSame(ProcessingStatus.PENDING, ProcessingStatus.valueOf("PENDING"));
        assertSame(ProcessingStatus.PROCESSING, ProcessingStatus.valueOf("PROCESSING"));
        assertSame(ProcessingStatus.COMPLETED, ProcessingStatus.valueOf("COMPLETED"));
        assertSame(ProcessingStatus.FAILED, ProcessingStatus.valueOf("FAILED"));
    }
}
