package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaxIdentifier value object
 */
class TaxIdentifierTest {

    @Test
    void of_createsTaxId() {
        // Given
        String taxId = "1234567890123";
        String scheme = "VAT";

        // When
        TaxIdentifier taxIdentifier = TaxIdentifier.of(taxId, scheme);

        // Then
        assertNotNull(taxIdentifier);
        assertEquals(taxId, taxIdentifier.taxId());
        assertEquals(scheme, taxIdentifier.scheme());
    }

    @Test
    void of_nullTaxId_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            TaxIdentifier.of(null, "VAT")
        );
    }

    @Test
    void of_nullScheme_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            TaxIdentifier.of("1234567890123", null)
        );
    }

    @Test
    void record_equality() {
        // Given
        TaxIdentifier taxId1 = TaxIdentifier.of("1234567890123", "VAT");
        TaxIdentifier taxId2 = TaxIdentifier.of("1234567890123", "VAT");
        TaxIdentifier taxId3 = TaxIdentifier.of("9876543210987", "VAT");
        TaxIdentifier taxId4 = TaxIdentifier.of("1234567890123", "EIN");

        // When/Then
        assertEquals(taxId1, taxId2);
        assertNotEquals(taxId1, taxId3);
        assertNotEquals(taxId1, taxId4);
    }

    @Test
    void record_hashCode() {
        // Given
        TaxIdentifier taxId1 = TaxIdentifier.of("1234567890123", "VAT");
        TaxIdentifier taxId2 = TaxIdentifier.of("1234567890123", "VAT");

        // When/Then
        assertEquals(taxId1.hashCode(), taxId2.hashCode());
    }

    @Test
    void of_differentSchemes() {
        // Given
        TaxIdentifier vatId = TaxIdentifier.of("1234567890", "VAT");
        TaxIdentifier einId = TaxIdentifier.of("9876543210", "EIN");
        TaxIdentifier tinId = TaxIdentifier.of("5555555555", "TIN");

        // When/Then
        assertNotEquals(vatId, einId);
        assertNotEquals(vatId, tinId);
        assertNotEquals(einId, tinId);
    }
}
