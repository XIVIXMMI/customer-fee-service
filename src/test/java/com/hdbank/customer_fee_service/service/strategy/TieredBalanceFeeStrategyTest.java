package com.hdbank.customer_fee_service.service.strategy;

import com.hdbank.customer_fee_service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TieredBalanceFeeStrategyTest {

    private TieredBalanceFeeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new TieredBalanceFeeStrategy();
    }

    @Test
    void shouldCalculateFeeForMatchingTier() {
        // Given - balance 30M falls in first tier (0-50M)
        Map<String, Object> params = new HashMap<>();
        params.put("balance", 30000000);
        params.put("tiers", Arrays.asList(
                Map.of("from", 0, "to", 50000000, "fee", 10000),
                Map.of("from", 50000001, "to", 200000000, "fee", 20000)
        ));

        // When
        BigDecimal result = strategy.calculateFee(BigDecimal.ZERO, params);

        // Then
        assertEquals(new BigDecimal("10000"), result);
    }

    @Test
    void shouldFallbackToMonthlyFeeWhenNoTierMatches() {
        // Given - balance 500M doesn't match any tier
        Map<String, Object> params = new HashMap<>();
        params.put("balance", 500000000);
        params.put("tiers", List.of(
                Map.of("from", 0, "to", 50000000, "fee", 10000)
        ));
        BigDecimal defaultFee = new BigDecimal("99999");

        // When
        BigDecimal result = strategy.calculateFee(defaultFee, params);

        // Then
        assertEquals(defaultFee, result);
    }

    @Test
    void shouldThrowExceptionWhenMissingBalance() {
        // Given - missing balance
        Map<String, Object> params = new HashMap<>();
        params.put("tiers", List.of(Map.of("from", 0, "fee", 10000)));

        // When/Then
        assertThrows(ValidationException.class, () -> strategy.validateParams(params));
    }

    @Test
    void shouldThrowExceptionWhenTiersEmpty() {
        // Given - empty tiers
        Map<String, Object> params = new HashMap<>();
        params.put("balance", 10000);
        params.put("tiers", List.of());

        // When/Then
        assertThrows(ValidationException.class, () -> strategy.validateParams(params));
    }
}