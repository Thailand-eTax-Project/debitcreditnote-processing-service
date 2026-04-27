package com.wpanther.debitcreditnote.processing.domain.model;

import java.util.Objects;
import java.util.UUID;

public class DebitCreditNoteId {
    private final UUID value;

    private DebitCreditNoteId(UUID value) {
        this.value = Objects.requireNonNull(value, "Debit/Credit Note ID cannot be null");
    }

    public static DebitCreditNoteId generate() {
        return new DebitCreditNoteId(UUID.randomUUID());
    }

    public static DebitCreditNoteId of(UUID value) {
        return new DebitCreditNoteId(value);
    }

    public static DebitCreditNoteId of(String value) {
        return new DebitCreditNoteId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DebitCreditNoteId that = (DebitCreditNoteId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
