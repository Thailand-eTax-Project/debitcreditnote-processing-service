package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LineItem value object
 */
class LineItemTest {

    @Test
    void construction_setsFields() {
        // Given
        String description = "Professional Services";
        int quantity = 10;
        Money unitPrice = Money.of(new BigDecimal("5000.00"), "THB");
        BigDecimal taxRate = new BigDecimal("7.00");

        // When
        LineItem lineItem = new LineItem(description, quantity, unitPrice, taxRate);

        // Then
        assertNotNull(lineItem);
        assertEquals(description, lineItem.description());
        assertEquals(quantity, lineItem.quantity());
        assertEquals(unitPrice, lineItem.unitPrice());
        assertEquals(taxRate, lineItem.taxRate());
    }

    @Test
    void construction_zeroQuantity_throws() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new LineItem("Services", 0, Money.of(new BigDecimal("100.00"), "THB"), new BigDecimal("7.00"))
        );
    }

    @Test
    void construction_negativeQuantity_throws() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            new LineItem("Services", -5, Money.of(new BigDecimal("100.00"), "THB"), new BigDecimal("7.00"))
        );
    }

    @Test
    void construction_nullDescription_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            new LineItem(null, 10, Money.of(new BigDecimal("100.00"), "THB"), new BigDecimal("7.00"))
        );
    }

    @Test
    void construction_nullUnitPrice_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            new LineItem("Services", 10, null, new BigDecimal("7.00"))
        );
    }

    @Test
    void construction_nullTaxRate_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () ->
            new LineItem("Services", 10, Money.of(new BigDecimal("100.00"), "THB"), null)
        );
    }

    @Test
    void getLineTotal_returnsQuantityTimesPrice() {
        // Given
        LineItem lineItem = new LineItem(
            "Services",
            10,
            Money.of(new BigDecimal("5000.00"), "THB"),
            new BigDecimal("7.00")
        );

        // When
        Money lineTotal = lineItem.getLineTotal();

        // Then
        assertEquals(Money.of(new BigDecimal("50000.00"), "THB"), lineTotal);
    }

    @Test
    void getTaxAmount_returnsCorrectTax() {
        // Given
        LineItem lineItem = new LineItem(
            "Services",
            10,
            Money.of(new BigDecimal("5000.00"), "THB"),
            new BigDecimal("7.00")
        );

        // When
        Money taxAmount = lineItem.getTaxAmount();

        // Then
        // Tax: 50000 * (7.00 / 100) = 3500
        // Money.multiply does not normalize scale: result is 3500.0000
        assertEquals(0, new BigDecimal("3500.00").compareTo(taxAmount.amount()));
        assertEquals("THB", taxAmount.currency());
    }

    @Test
    void getTotalWithTax_returnsLineTotalPlusTax() {
        // Given
        LineItem lineItem = new LineItem(
            "Services",
            10,
            Money.of(new BigDecimal("5000.00"), "THB"),
            new BigDecimal("7.00")
        );

        // When
        Money totalWithTax = lineItem.getTotalWithTax();

        // Then
        // Total: 50000 + 3500.0000 = 53500.0000 (scale from tax calculation)
        assertEquals(0, new BigDecimal("53500.00").compareTo(totalWithTax.amount()));
        assertEquals("THB", totalWithTax.currency());
    }

    @Test
    void zeroTaxRate_producesZeroTax() {
        // Given
        LineItem lineItem = new LineItem(
            "Services",
            5,
            Money.of(new BigDecimal("1000.00"), "THB"),
            BigDecimal.ZERO
        );

        // When
        Money taxAmount = lineItem.getTaxAmount();
        Money totalWithTax = lineItem.getTotalWithTax();

        // Then
        assertEquals(Money.of(new BigDecimal("0.00"), "THB"), taxAmount);
        assertEquals(Money.of(new BigDecimal("5000.00"), "THB"), totalWithTax);
    }

    @Test
    void singleQuantity_lineTotalEqualsUnitPrice() {
        // Given
        LineItem lineItem = new LineItem(
            "Software License",
            1,
            Money.of(new BigDecimal("10000.00"), "THB"),
            new BigDecimal("7.00")
        );

        // When
        Money lineTotal = lineItem.getLineTotal();

        // Then
        assertEquals(Money.of(new BigDecimal("10000.00"), "THB"), lineTotal);
    }

    @Test
    void largeQuantity() {
        // Given
        LineItem lineItem = new LineItem(
            "Small Items",
            1000,
            Money.of(new BigDecimal("5.00"), "THB"),
            new BigDecimal("7.00")
        );

        // When
        Money lineTotal = lineItem.getLineTotal();

        // Then
        assertEquals(Money.of(new BigDecimal("5000.00"), "THB"), lineTotal);
    }

    @Test
    void record_equality() {
        // Given
        LineItem item1 = new LineItem("Services", 10, Money.of(new BigDecimal("100.00"), "THB"), new BigDecimal("7.00"));
        LineItem item2 = new LineItem("Services", 10, Money.of(new BigDecimal("100.00"), "THB"), new BigDecimal("7.00"));
        LineItem item3 = new LineItem("Products", 10, Money.of(new BigDecimal("100.00"), "THB"), new BigDecimal("7.00"));

        // When/Then
        assertEquals(item1, item2);
        assertNotEquals(item1, item3);
    }

    @Test
    void record_hashCode() {
        // Given
        LineItem item1 = new LineItem("Services", 10, Money.of(new BigDecimal("100.00"), "THB"), new BigDecimal("7.00"));
        LineItem item2 = new LineItem("Services", 10, Money.of(new BigDecimal("100.00"), "THB"), new BigDecimal("7.00"));

        // When/Then
        assertEquals(item1.hashCode(), item2.hashCode());
    }
}
