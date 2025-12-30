package com.hdbank.customer_fee_service.repository;

import com.hdbank.customer_fee_service.entity.CustomerFeeJob;
import com.hdbank.customer_fee_service.entity.FeeJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerFeeJobRepository extends JpaRepository<CustomerFeeJob, Long> {

    Optional<CustomerFeeJob> findByIdempotencyKey(String idempotencyKey);
    List<CustomerFeeJob> findByStatus(FeeJobStatus status, Pageable pageable);
    List<CustomerFeeJob> findByCustomerIdAndBillingMonth(Long customerId, String billingMonth);
    List<CustomerFeeJob> findByCustomerId(Long customerId);
    boolean existsByIdempotencyKey(String idempotencyKey);
    List<CustomerFeeJob> findByBillingMonth(String billingMonth);
}
