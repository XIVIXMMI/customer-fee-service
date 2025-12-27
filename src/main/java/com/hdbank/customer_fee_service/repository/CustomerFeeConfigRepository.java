package com.hdbank.customer_fee_service.repository;

import com.hdbank.customer_fee_service.entity.CustomerFeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerFeeConfigRepository extends JpaRepository<CustomerFeeConfig, Long> {

    List<CustomerFeeConfig> findByCustomerId(Long customerId);

    /**
     * Find the active CustomerFeeConfig for a given customerId and date.
     * An active config is defined as one where:
     * - deletedAt is null
     * - effectiveFrom is on or before the given date
     * - effectiveTo is null or on or after the given date
     * @param customerId the ID of the customer
     * @param date the date to check the effectiveness
     * @return an Optional containing the active CustomerFeeConfig if found, otherwise empty
     */
    @Query("SELECT c FROM CustomerFeeConfig c " +
            "WHERE c.customerId = :customerId " +
            "AND c.deletedAt IS NULL " +
            "AND c.effectiveFrom <= :date " +
            "AND (c.effectiveTo IS NULL OR c.effectiveTo >= :date)")
    Optional<CustomerFeeConfig> findActiveConfigByCustomerIdAndDate(
            @Param("customerId") Long customerId,
            @Param("date") LocalDate date
    );

    List<CustomerFeeConfig> findByCustomerIdAndDeletedAtIsNull(Long customerId);
}
