package com.wpanther.debitcreditnote.processing.domain.model;

import java.util.Objects;

public record Address(String street, String city, String postalCode, String country) {

    public static Address of(String street, String city, String postalCode, String country) {
        Objects.requireNonNull(country, "Country is required");
        return new Address(street, city, postalCode, country);
    }
}
