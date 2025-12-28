package com.hdbank.customer_fee_service.repository;

import com.hdbank.customer_fee_service.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);
    List<Customer> findByStatus(String status);
    List<Customer> findByStatusAndDeletedAtIsNull(String status);
    boolean existsByEmail(String email);
    Page<Customer> findByDeletedAtIsNull(Pageable pageable);
}
