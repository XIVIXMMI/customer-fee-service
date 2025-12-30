package com.hdbank.customer_fee_service.service;

import com.hdbank.customer_fee_service.entity.CustomerFeeJob;
import com.hdbank.customer_fee_service.entity.FeeJobStatus;
import com.hdbank.customer_fee_service.repository.CustomerFeeJobRepository;
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
}
