package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DebitCreditNoteId value object
 */
class DebitCreditNoteIdTest {

    @Test
    void generate_createsNonNullId() {
        // When
        DebitCreditNoteId id = DebitCreditNoteId.generate();

        // Then
        assertNotNull(id);
        assertNotNull(id.getValue());
    }

    @Test
    void generate_createsUniqueIds() {
        // When
        DebitCreditNoteId id1 = DebitCreditNoteId.generate();
        DebitCreditNoteId id2 = DebitCreditNoteId.generate();

        // Then
        assertNotEquals(id1, id2);
        assertNotEquals(id1.getValue(), id2.getValue());
    }

    @Test
    void ofUuid_createsIdWithValue() {
        // Given
        UUID uuid = UUID.randomUUID();

        // When
        DebitCreditNoteId id = DebitCreditNoteId.of(uuid);

        // Then
        assertNotNull(id);
        assertEquals(uuid, id.getValue());
    }

    @Test
    void ofString_createsIdFromUuidString() {
        // Given
        String uuidString = "550e8400-e29b-41d4-a716-446655440000";

        // When
        DebitCreditNoteId id = DebitCreditNoteId.of(uuidString);

        // Then
        assertNotNull(id);
        assertEquals(UUID.fromString(uuidString), id.getValue());
    }

    @Test
    void ofNull_throwsNPE() {
        // When/Then
        assertThrows(NullPointerException.class, () -> DebitCreditNoteId.of((UUID) null));
        assertThrows(NullPointerException.class, () -> DebitCreditNoteId.of((String) null));
    }

    @Test
    void equals_sameValue() {
        // Given
        UUID uuid = UUID.randomUUID();
        DebitCreditNoteId id1 = DebitCreditNoteId.of(uuid);
        DebitCreditNoteId id2 = DebitCreditNoteId.of(uuid);
        DebitCreditNoteId id3 = DebitCreditNoteId.generate();

        // When/Then
        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
    }

    @Test
    void hashCode_sameValue() {
        // Given
        UUID uuid = UUID.randomUUID();
        DebitCreditNoteId id1 = DebitCreditNoteId.of(uuid);
        DebitCreditNoteId id2 = DebitCreditNoteId.of(uuid);

        // When/Then
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void toString_returnsUuidString() {
        // Given
        String uuidString = "550e8400-e29b-41d4-a716-446655440000";
        DebitCreditNoteId id = DebitCreditNoteId.of(uuidString);

        // When
        String result = id.toString();

        // Then
        assertEquals(uuidString, result);
    }

    @Test
    void ofAndToString_roundTrip() {
        // Given
        DebitCreditNoteId original = DebitCreditNoteId.generate();
        String stringRepresentation = original.toString();

        // When
        DebitCreditNoteId reconstructed = DebitCreditNoteId.of(stringRepresentation);

        // Then
        assertEquals(original, reconstructed);
        assertEquals(original.getValue(), reconstructed.getValue());
    }
}
