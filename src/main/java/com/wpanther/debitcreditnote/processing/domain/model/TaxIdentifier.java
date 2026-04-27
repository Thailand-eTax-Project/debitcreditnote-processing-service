package com.wpanther.debitcreditnote.processing.domain.model;

import java.util.Objects;

public record TaxIdentifier(String taxId, String scheme) {

    public TaxIdentifier {
        Objects.requireNonNull(taxId, "Tax ID is required");
        Objects.requireNonNull(scheme, "Scheme is required");
    }

    public static TaxIdentifier of(String taxId, String scheme) {
        return new TaxIdentifier(taxId, scheme);
    }
}
