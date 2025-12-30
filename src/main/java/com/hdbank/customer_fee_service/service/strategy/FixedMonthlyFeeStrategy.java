package com.hdbank.customer_fee_service.service.strategy;

import com.hdbank.customer_fee_service.entity.CalculationType;
import com.hdbank.customer_fee_service.entity.FeeJobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class FixedMonthlyFeeStrategy implements FeeCalculationStrategy{

    @Override
    public BigDecimal calculateFee(BigDecimal monthlyFeeAmount, Map<String, Object> calculationParams) {
        log.info("Calculating fixed monthly fee: {}", monthlyFeeAmount);
        return monthlyFeeAmount;
    }

    @Override
    public String getCalculationType() {
        return CalculationType.FIXED.toString();
    }

    @Override
    public void validateParams(Map<String, Object> calculationParams) {
        // Because it is a fixed monthly fee, no additional parameters are required
        log.info("No additional parameters required for FIXED calculation type");
    }
}
