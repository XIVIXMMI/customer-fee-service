package com.hdbank.customer_fee_service.service;

import com.hdbank.customer_fee_service.dto.response.FeeChargeResult;
import com.hdbank.customer_fee_service.entity.*;
import com.hdbank.customer_fee_service.exception.BusinessException;
import com.hdbank.customer_fee_service.exception.EntityNotFoundException;
import com.hdbank.customer_fee_service.repository.*;
import com.hdbank.customer_fee_service.service.strategy.FeeCalculationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeChargeService {

    private static final String FIRST_DAY_OF_MONTH = "01"; //dd

    private final CustomerFeeJobRepository feeJobRepository;
    private final CustomerFeeConfigRepository feeConfigRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final CustomerRepository customerRepository;
    private final FeeChargedAttemptRepository feeChargedAttemptRepository;
    private final FeeCalculationContext feeCalculationContext;

    /**
     * Charge fee for a specified job
     * Flow:
     * 1. Get job from DB
     * 2. Validate job status = NEW
     * 3. Update status to IN_PROGRESS
     * 4. Get customer, fee config, fee type
     * 5. Calculate fee using strategy pattern
     * 6. Update job with amount and status DONE
     * 7. Return result
     */
    @Transactional
    public FeeChargeResult chargeFee(Long jobId) {
        log.info("Charging fee for job: {}", jobId);

        CustomerFeeJob job = feeJobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + jobId));

        // Must be "New"
        if (!FeeJobStatus.NEW.equals(job.getStatus())) {
            throw new BusinessException("JOB_INVALID_STATUS",
                    "Job is not in NEW status: " + job.getStatus());
        }

        try {
            job.setStatus(FeeJobStatus.IN_PROGRESS);
            feeJobRepository.save(job);

            Customer customer = customerRepository.findById(job.getCustomerId())
                    .filter(c -> c.getDeletedAt() == null)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Customer not found with id: " + job.getCustomerId()));

            // Parse billing date (from "2025-01" to "2025-01-01")
            LocalDate billingDate = LocalDate.parse(job.getBillingMonth() + "-" + FIRST_DAY_OF_MONTH);

            CustomerFeeConfig config = feeConfigRepository
                    .findActiveConfigByCustomerIdAndDate(job.getCustomerId(), billingDate)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No active fee config for customer: " + job.getCustomerId()));

            FeeType feeType = feeTypeRepository.findById(config.getFeeTypeId())
                    .filter(FeeType::getIsActive)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Fee type not found with id: " + config.getFeeTypeId()));

            Map<String, Object> params = config.getCalculationParams() != null
                    ? config.getCalculationParams()
                    : new HashMap<>();

            BigDecimal calculatedFee = feeCalculationContext.calculateFee(
                    feeType.getCalculationType(),
                    config.getMonthlyFeeAmount(),
                    params
            );

            log.info("Calculated fee for customer {}: {} {}",
                    customer.getId(), calculatedFee, config.getCurrency());

            // TODO: Deduct from customer balance (call payment service - implement later)
            log.info("Deducting {} {} from customer {}",
                    calculatedFee, config.getCurrency(), customer.getId());

            job.setStatus(FeeJobStatus.DONE);
            job.setAmount(calculatedFee);
            feeJobRepository.save(job);

            log.info("Fee charged successfully for job: {}", jobId);

            return FeeChargeResult.builder()
                    .jobId(jobId)
                    .customerId(customer.getId())
                    .feeConfigId(config.getId())  // Add this field
                    .chargedAmount(calculatedFee)
                    .currency(config.getCurrency())
                    .billingMonth(job.getBillingMonth())
                    .status("SUCCESS")
                    .chargedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Error charging fee for job: {}", jobId, e);

            job.setStatus(FeeJobStatus.FAILED);
            // TODO: Entity has no errorMessage field, could add to migration later if needed
            feeJobRepository.save(job);

            return FeeChargeResult.builder()
                    .jobId(jobId)
                    .customerId(job.getCustomerId())
                    .billingMonth(job.getBillingMonth())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .chargedAt(Instant.now())
                    .build();
        }
    }
}
