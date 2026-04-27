package com.wpanther.debitcreditnote.processing.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record LineItem(String description, int quantity, Money unitPrice, BigDecimal taxRate) {

    public LineItem {
        Objects.requireNonNull(description, "Line item description is required");
        Objects.requireNonNull(unitPrice, "Unit price is required");
        Objects.requireNonNull(taxRate, "Tax rate is required");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }

    public Money getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Money getTaxAmount() {
        return getLineTotal().multiply(taxRate.divide(BigDecimal.valueOf(100)));
    }

    public Money getTotalWithTax() {
        return getLineTotal().add(getTaxAmount());
    }
}
