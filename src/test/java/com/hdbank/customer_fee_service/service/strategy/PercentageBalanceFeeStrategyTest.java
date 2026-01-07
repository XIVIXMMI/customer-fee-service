package com.hdbank.customer_fee_service.service.strategy;

import com.hdbank.customer_fee_service.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PercentageBalanceFeeStrategyTest {

    private PercentageBalanceFeeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PercentageBalanceFeeStrategy();
    }

    @Test
    void shouldCalculatePercentageFee() {
        // Given - 100M * 0.1% = 100k
        Map<String, Object> params = new HashMap<>();
        params.put("balance", 100000000);
        params.put("percentage", 0.001);

        BigDecimal result = strategy.calculateFee(BigDecimal.ZERO, params);

        assertEquals(new BigDecimal("100000.00"), result);
    }

    @Test
    void shouldApplyMinimumCap() {
        // Given - 10M * 0.1% = 10k, but min is 20k
        Map<String, Object> params = new HashMap<>();
        params.put("balance", 10000000);
        params.put("percentage", 0.001);
        params.put("min_fee", 20000);

        BigDecimal result = strategy.calculateFee(BigDecimal.ZERO, params);

        assertEquals(new BigDecimal("20000"), result);
    }

    @Test
    void shouldApplyMaximumCap() {
        // Given - 1B * 0.1% = 1M, but max is 100k
        Map<String, Object> params = new HashMap<>();
        params.put("balance", 1000000000);
        params.put("percentage", 0.001);
        params.put("max_fee", 100000);

        BigDecimal result = strategy.calculateFee(BigDecimal.ZERO, params);

        assertEquals(new BigDecimal("100000"), result);
    }

    @Test
    void shouldThrowExceptionWhenPercentageInvalid() {
        // Given - percentage > 1
        Map<String, Object> params = new HashMap<>();
        params.put("balance", 100000);
        params.put("percentage", 1.5);

        assertThrows(ValidationException.class, () -> strategy.validateParams(params));
    }

    @Test
    void shouldThrowExceptionWhenMissingPercentage() {
        // Given - missing percentage
        Map<String, Object> params = new HashMap<>();
        params.put("balance", 100000);

        assertThrows(ValidationException.class, () -> strategy.validateParams(params));
    }
}