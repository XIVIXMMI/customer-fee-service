package com.hdbank.customer_fee_service.service;

import com.hdbank.customer_fee_service.entity.FeeChargeAttempt;
import com.hdbank.customer_fee_service.repository.FeeChargedAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeChargeAttemptService {

    private final FeeChargedAttemptRepository attemptRepository;

    public List<FeeChargeAttempt> getAttemptsByJobId(Long jobId) {
        log.info("Getting fee charge attempts for job: {}", jobId);
        return attemptRepository.findByJobIdOrderByAttemptNoAsc(jobId);
    }

    public List<FeeChargeAttempt> getAttemptsByCustomerId(Long customerId) {
        log.info("Getting fee charge attempts for customer: {}", customerId);
        return attemptRepository.findByCustomerId(customerId);
    }

    public List<FeeChargeAttempt> getFailedAttempts() {
        log.info("Getting all failed fee charge attempts");
        return attemptRepository.findByStatusOrderByCreatedAtDesc("FAILED");
    }

    public List<FeeChargeAttempt> getFailedAttemptsByMonth(String billingMonth) {
        log.info("Getting failed attempts for billing month: {}", billingMonth);
        return attemptRepository.findByBillingMonthAndStatusOrderByCreatedAtDesc(billingMonth, "FAILED");
    }
}