package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Address value object
 */
class AddressTest {

    @Test
    void of_createsAddress() {
        // When
        Address address = Address.of("123 Street", "Bangkok", "10110", "TH");

        // Then
        assertNotNull(address);
        assertEquals("123 Street", address.street());
        assertEquals("Bangkok", address.city());
        assertEquals("10110", address.postalCode());
        assertEquals("TH", address.country());
    }

    @Test
    void of_nullCountry_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            Address.of("123 Street", "Bangkok", "10110", null)
        );
    }

    @Test
    void of_allowsNullOptionalFields() {
        // When
        Address address = Address.of(null, null, null, "TH");

        // Then
        assertNotNull(address);
        assertNull(address.street());
        assertNull(address.city());
        assertNull(address.postalCode());
        assertEquals("TH", address.country());
    }

    @Test
    void record_equality() {
        // Given
        Address address1 = Address.of("123 Street", "Bangkok", "10110", "TH");
        Address address2 = Address.of("123 Street", "Bangkok", "10110", "TH");
        Address address3 = Address.of("456 Street", "Bangkok", "10110", "TH");

        // When/Then
        assertEquals(address1, address2);
        assertNotEquals(address1, address3);
    }

    @Test
    void record_hashCode() {
        // Given
        Address address1 = Address.of("123 Street", "Bangkok", "10110", "TH");
        Address address2 = Address.of("123 Street", "Bangkok", "10110", "TH");

        // When/Then
        assertEquals(address1.hashCode(), address2.hashCode());
    }

    @Test
    void constructor_directConstruction() {
        // When - bypassing of() factory to verify compact constructor does NOT enforce null country
        Address address = new Address("456 Road", "Chiang Mai", "50000", "US");

        // Then
        assertEquals("456 Road", address.street());
        assertEquals("Chiang Mai", address.city());
        assertEquals("50000", address.postalCode());
        assertEquals("US", address.country());
    }
}
