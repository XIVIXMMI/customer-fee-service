package com.hdbank.customer_fee_service.repository;

import com.hdbank.customer_fee_service.entity.FeeChargeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeChargedAttemptRepository extends JpaRepository<FeeChargeAttempt, Long> {

    List<FeeChargeAttempt> findByJobIdOrderByAttemptNoAsc(Long jobId);
    List<FeeChargeAttempt> findByCustomerId(Long customerId);
    List<FeeChargeAttempt> findByStatus(String status);
    List<FeeChargeAttempt> findByStatusOrderByCreatedAtDesc(String status);
}
