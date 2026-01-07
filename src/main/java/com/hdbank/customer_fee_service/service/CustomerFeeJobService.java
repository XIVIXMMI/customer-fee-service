package com.hdbank.customer_fee_service.service;

import com.hdbank.customer_fee_service.dto.response.FeeChargeStatsResponse;
import com.hdbank.customer_fee_service.entity.AttemptStatus;
import com.hdbank.customer_fee_service.entity.CustomerFeeJob;
import com.hdbank.customer_fee_service.entity.FeeChargeAttempt;
import com.hdbank.customer_fee_service.entity.FeeJobStatus;
import com.hdbank.customer_fee_service.exception.EntityNotFoundException;
import com.hdbank.customer_fee_service.repository.CustomerFeeJobRepository;
import com.hdbank.customer_fee_service.repository.FeeChargedAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerFeeJobService {

    private final CustomerFeeJobRepository feeJobRepository;
    private final FeeChargedAttemptRepository feeChargedAttemptRepository;

    public List<CustomerFeeJob> getJobsByCustomerId(Long customerId){
        log.info("Getting Fee Job for customer with id: {}", customerId);
        return feeJobRepository.findByCustomerId(customerId);
    }

    public List<CustomerFeeJob> getJobsByStatus(String status, int page, int size){
        log.info("Getting Fee Jobs with status: {} (page: {} size: {}", status, page, size);
        FeeJobStatus feeJobStatus = FeeJobStatus.valueOf(status);
        return feeJobRepository
                .findByStatus(
                        feeJobStatus,
                        PageRequest.of(page, size, Sort.by("createdAt").ascending()));
    }

    public List<CustomerFeeJob> getJobsByBillingMonth(String billingMonth){
        log.info("Getting Fee Jobs for billing month: {}",billingMonth);
        return feeJobRepository.findByBillingMonth(billingMonth);
    }

    public CustomerFeeJob getJobById(Long jobId){
        log.info("Getting job with id: {}", jobId);
        return feeJobRepository.findById(jobId)
                .filter( j -> j.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
    }

    public List<CustomerFeeJob> getJobsByCustomerIdAndMonth(Long customerId, String billingMonth){
        log.info("Getting jobs for customer: {} and month {}", customerId, billingMonth);
        return feeJobRepository.findByCustomerIdAndBillingMonth(customerId,billingMonth);
    }


    public FeeChargeStatsResponse getStatsByBillingMonth(String billingMonth) {
        log.info("Getting fee charge stats for month: {}", billingMonth);

        List<CustomerFeeJob> allJobs = feeJobRepository.findByBillingMonth(billingMonth);

        long totalJobs = allJobs.size();
        long jobsDone = allJobs.stream().filter(j -> j.getStatus() == FeeJobStatus.DONE).count();
        long jobsFailed = allJobs.stream().filter(j -> j.getStatus() == FeeJobStatus.FAILED).count();
        long jobsPending = allJobs.stream().filter(j -> j.getStatus() == FeeJobStatus.NEW).count();

        // Get all attempts for this month
        List<FeeChargeAttempt> attempts = feeChargedAttemptRepository.findByBillingMonth(billingMonth);
        long totalAttempts = attempts.size();
        long attemptsSuccess = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.SUCCESS).count();
        long attemptsFailed = attempts.stream().filter(a -> a.getStatus() == AttemptStatus.FAILED).count();

        return FeeChargeStatsResponse.builder()
                .billingMonth(billingMonth)
                .totalJobs(totalJobs)
                .jobsDone(jobsDone)
                .jobsFailed(jobsFailed)
                .jobsPending(jobsPending)
                .totalAttempts(totalAttempts)
                .attemptsSuccess(attemptsSuccess)
                .attemptsFailed(attemptsFailed)
                .build();
    }
}
