package com.hdbank.customer_fee_service.service.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FixedMonthlyFeeStrategyTest {

    private FixedMonthlyFeeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FixedMonthlyFeeStrategy();
    }

    @Test
    void shouldReturnMonthlyFeeAmount() {
        BigDecimal monthlyFee = new BigDecimal("50000");
        Map<String, Object> params = new HashMap<>();

        BigDecimal result = strategy.calculateFee(monthlyFee, params);

        assertEquals(monthlyFee, result);
    }

    @Test
    void shouldHandleNullParams() {
        BigDecimal monthlyFee = new BigDecimal("50000");

        BigDecimal result = strategy.calculateFee(monthlyFee, null);

        assertEquals(monthlyFee, result);
    }

    @Test
    void shouldValidateWithNoRequiredParams() {
        // Given
        Map<String, Object> params = new HashMap<>();

        // When/Then - sVould not throw exception
        assertDoesNotThrow(() -> strategy.validateParams(params));
    }
}