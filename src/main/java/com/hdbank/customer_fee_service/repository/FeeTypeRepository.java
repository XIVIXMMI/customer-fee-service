package com.hdbank.customer_fee_service.repository;

import com.hdbank.customer_fee_service.entity.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeTypeRepository extends JpaRepository<FeeType, Long> {

    Optional<FeeType> findByCode(String code);
    List<FeeType> findByIsActiveTrue();
    List<FeeType> findByCalculationType(String calculationType);
}
