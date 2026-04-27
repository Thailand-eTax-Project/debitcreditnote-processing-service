package com.wpanther.debitcreditnote.processing.domain.model;

import java.util.Objects;

public record Party(String name, TaxIdentifier taxIdentifier, Address address, String email) {

    public Party {
        Objects.requireNonNull(name, "Party name is required");
        Objects.requireNonNull(taxIdentifier, "Tax identifier is required");
        Objects.requireNonNull(address, "Address is required");
    }

    public static Party of(String name, TaxIdentifier taxIdentifier, Address address, String email) {
        return new Party(name, taxIdentifier, address, email);
    }
}
