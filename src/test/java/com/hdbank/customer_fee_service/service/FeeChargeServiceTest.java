package com.hdbank.customer_fee_service.service;

import com.hdbank.customer_fee_service.dto.response.FeeChargeResult;
import com.hdbank.customer_fee_service.entity.Customer;
import com.hdbank.customer_fee_service.entity.CustomerFeeConfig;
import com.hdbank.customer_fee_service.entity.CustomerFeeJob;
import com.hdbank.customer_fee_service.entity.FeeJobStatus;
import com.hdbank.customer_fee_service.entity.FeeType;
import com.hdbank.customer_fee_service.exception.BusinessException;
import com.hdbank.customer_fee_service.exception.EntityNotFoundException;
import com.hdbank.customer_fee_service.repository.CustomerFeeConfigRepository;
import com.hdbank.customer_fee_service.repository.CustomerFeeJobRepository;
import com.hdbank.customer_fee_service.repository.CustomerRepository;
import com.hdbank.customer_fee_service.repository.FeeChargedAttemptRepository;
import com.hdbank.customer_fee_service.repository.FeeTypeRepository;
import com.hdbank.customer_fee_service.service.strategy.FeeCalculationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeChargeServiceTest {

    @Mock
    private CustomerFeeJobRepository feeJobRepository;

    @Mock
    private CustomerFeeConfigRepository feeConfigRepository;

    @Mock
    private FeeTypeRepository feeTypeRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private FeeChargedAttemptRepository feeChargedAttemptRepository;

    @Mock
    private FeeCalculationContext feeCalculationContext;

    @InjectMocks
    private FeeChargeService feeChargeService;

    private CustomerFeeJob mockJob;
    private Customer mockCustomer;
    private CustomerFeeConfig mockConfig;
    private FeeType mockFeeType;

    @BeforeEach
    void setUp() {
        mockJob = CustomerFeeJob.builder()
                .id(1L)
                .customerId(100L)
                .billingMonth("2025-01")
                .status(FeeJobStatus.NEW)
                .build();

        mockCustomer = Customer.builder()
                .id(100L)
                .fullName("Nguyen Van A")
                .email("nguyenvana@example.com")
                .phoneNumber("0123456789")
                .status("ACTIVE")
                .build();

        mockConfig = CustomerFeeConfig.builder()
                .id(1L)
                .customerId(100L)
                .feeTypeId(1L)
                .monthlyFeeAmount(new BigDecimal("50000"))
                .currency("VND")
                .effectiveFrom(LocalDate.now().minusMonths(1))
                .calculationParams(new HashMap<>())
                .build();

        mockFeeType = FeeType.builder()
                .id(1L)
                .code("FIXED_MONTHLY")
                .calculationType("FIXED")
                .isActive(true)
                .build();
    }

    @Test
    void shouldThrowExceptionWhenJobNotFound() {
        when(feeJobRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> feeChargeService.chargeFee(999L));
    }

    @Test
    void shouldThrowExceptionWhenJobStatusNotNew() {
        mockJob.setStatus(FeeJobStatus.DONE);
        when(feeJobRepository.findById(1L)).thenReturn(Optional.of(mockJob));

        assertThrows(BusinessException.class,
                () -> feeChargeService.chargeFee(1L));
    }

    @Test
    void shouldChargeFeSuccessfully() {
        when(feeJobRepository.findById(1L)).thenReturn(Optional.of(mockJob));
        when(customerRepository.findById(100L)).thenReturn(Optional.of(mockCustomer));
        when(feeConfigRepository.findActiveConfigByCustomerIdAndDate(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.of(mockConfig));
        when(feeTypeRepository.findById(1L)).thenReturn(Optional.of(mockFeeType));
        when(feeCalculationContext.calculateFee(any(), any(), any()))
                .thenReturn(new BigDecimal("50000"));
        when(feeJobRepository.save(any())).thenReturn(mockJob);

        FeeChargeResult result = feeChargeService.chargeFee(1L);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals(100L, result.getCustomerId());
        assertEquals(new BigDecimal("50000"), result.getChargedAmount());
    }
}