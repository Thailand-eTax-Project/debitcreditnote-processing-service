package com.wpanther.debitcreditnote.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Money value object
 */
class MoneyTest {

    @Test
    void of_createsMoney() {
        // Given
        BigDecimal amount = new BigDecimal("100.50");
        String currency = "THB";

        // When
        Money money = Money.of(amount, currency);

        // Then
        assertNotNull(money);
        assertEquals(new BigDecimal("100.50"), money.amount());
        assertEquals("THB", money.currency());
    }

    @Test
    void of_invalidCurrency_throws() {
        // Given
        BigDecimal amount = BigDecimal.TEN;

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> Money.of(amount, "US"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(amount, "USDT"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(amount, ""));
    }

    @Test
    void of_nullAmount_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () -> Money.of(null, "THB"));
    }

    @Test
    void of_nullCurrency_throws() {
        // When/Then
        assertThrows(NullPointerException.class, () -> Money.of(BigDecimal.TEN, null));
    }

    @Test
    void add_sameCurrency() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"), "THB");
        Money money2 = Money.of(new BigDecimal("50.25"), "THB");

        // When
        Money result = money1.add(money2);

        // Then
        assertEquals(new BigDecimal("150.75"), result.amount());
        assertEquals("THB", result.currency());
    }

    @Test
    void add_differentCurrency_throws() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.00"), "THB");
        Money money2 = Money.of(new BigDecimal("50.00"), "USD");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> money1.add(money2));
        assertTrue(exception.getMessage().contains("Cannot add money with different currencies"));
    }

    @Test
    void subtract_sameCurrency() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"), "THB");
        Money money2 = Money.of(new BigDecimal("50.25"), "THB");

        // When
        Money result = money1.subtract(money2);

        // Then
        assertEquals(new BigDecimal("50.25"), result.amount());
        assertEquals("THB", result.currency());
    }

    @Test
    void subtract_differentCurrency_throws() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.00"), "THB");
        Money money2 = Money.of(new BigDecimal("50.00"), "USD");

        // When/Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> money1.subtract(money2));
        assertTrue(exception.getMessage().contains("Cannot subtract money with different currencies"));
    }

    @Test
    void multiply_returnsScaledResult() {
        // Given
        Money money = Money.of(new BigDecimal("100.00"), "THB");
        BigDecimal multiplier = new BigDecimal("1.5");

        // When
        Money result = money.multiply(multiplier);

        // Then - Money.multiply does not normalize scale; 100.00 * 1.5 = 150.000
        assertEquals(0, new BigDecimal("150.00").compareTo(result.amount()));
        assertEquals("THB", result.currency());
    }

    @Test
    void zero_createsZeroAmount() {
        // Given
        String currency = "EUR";

        // When
        Money money = Money.zero(currency);

        // Then
        assertNotNull(money);
        assertEquals(BigDecimal.ZERO, money.amount());
        assertEquals("EUR", money.currency());
    }

    @Test
    void equality_sameValue() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"), "THB");
        Money money2 = Money.of(new BigDecimal("100.50"), "THB");
        Money money3 = Money.of(new BigDecimal("100.51"), "THB");
        Money money4 = Money.of(new BigDecimal("100.50"), "USD");

        // When/Then
        assertEquals(money1, money2);
        assertNotEquals(money1, money3);
        assertNotEquals(money1, money4);
    }

    @Test
    void hashCode_sameValue() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.50"), "THB");
        Money money2 = Money.of(new BigDecimal("100.50"), "THB");

        // When/Then
        assertEquals(money1.hashCode(), money2.hashCode());
    }
}
