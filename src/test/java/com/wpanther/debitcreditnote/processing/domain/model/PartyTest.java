package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Party value object
 */
class PartyTest {

    @Test
    void of_createsParty() {
        // Given
        TaxIdentifier taxId = TaxIdentifier.of("1234567890123", "VAT");
        Address address = Address.of("123 Street", "Bangkok", "10110", "TH");

        // When
        Party party = Party.of("Acme Corporation", taxId, address, "info@acme.com");

        // Then
        assertNotNull(party);
        assertEquals("Acme Corporation", party.name());
        assertEquals(taxId, party.taxIdentifier());
        assertEquals(address, party.address());
        assertEquals("info@acme.com", party.email());
    }

    @Test
    void of_nullEmail_isAllowed() {
        // Given
        TaxIdentifier taxId = TaxIdentifier.of("1234567890123", "VAT");
        Address address = Address.of("123 Street", "Bangkok", "10110", "TH");

        // When
        Party party = Party.of("Test Company", taxId, address, null);

        // Then
        assertNotNull(party);
        assertNull(party.email());
    }

    @Test
    void of_nullName_throws() {
        // Given
        TaxIdentifier taxId = TaxIdentifier.of("1234567890123", "VAT");
        Address address = Address.of("123 Street", "Bangkok", "10110", "TH");

        // When/Then
        assertThrows(NullPointerException.class, () ->
            Party.of(null, taxId, address, null)
        );
    }

    @Test
    void of_nullTaxId_throws() {
        // Given
        Address address = Address.of("123 Street", "Bangkok", "10110", "TH");

        // When/Then
        assertThrows(NullPointerException.class, () ->
            Party.of("Test Company", null, address, null)
        );
    }

    @Test
    void of_nullAddress_throws() {
        // Given
        TaxIdentifier taxId = TaxIdentifier.of("1234567890123", "VAT");

        // When/Then
        assertThrows(NullPointerException.class, () ->
            Party.of("Test Company", taxId, null, null)
        );
    }

    @Test
    void record_equality() {
        // Given
        TaxIdentifier taxId = TaxIdentifier.of("1234567890123", "VAT");
        Address address = Address.of("123 Street", "Bangkok", "10110", "TH");

        Party party1 = Party.of("Company A", taxId, address, "email@test.com");
        Party party2 = Party.of("Company A", taxId, address, "email@test.com");
        Party party3 = Party.of("Company B", taxId, address, "email@test.com");

        // When/Then
        assertEquals(party1, party2);
        assertNotEquals(party1, party3);
    }

    @Test
    void record_hashCode() {
        // Given
        TaxIdentifier taxId = TaxIdentifier.of("1234567890123", "VAT");
        Address address = Address.of("123 Street", "Bangkok", "10110", "TH");

        Party party1 = Party.of("Company A", taxId, address, "email@test.com");
        Party party2 = Party.of("Company A", taxId, address, "email@test.com");

        // When/Then
        assertEquals(party1.hashCode(), party2.hashCode());
    }

    @Test
    void sellerAndBuyer_scenario() {
        // Given
        Party seller = Party.of(
            "Seller Company",
            TaxIdentifier.of("1111111111", "VAT"),
            Address.of("123 Seller St", "Bangkok", "10110", "TH"),
            "seller@company.com"
        );

        Party buyer = Party.of(
            "Buyer Company",
            TaxIdentifier.of("2222222222", "VAT"),
            Address.of("456 Buyer Rd", "Chiang Mai", "50000", "TH"),
            "buyer@company.com"
        );

        // When/Then
        assertNotEquals(seller, buyer);
        assertEquals("Seller Company", seller.name());
        assertEquals("Buyer Company", buyer.name());
    }
}
