package com.hdbank.customer_fee_service.service;

import com.hdbank.customer_fee_service.dto.request.CreateFeeConfigRequest;
import com.hdbank.customer_fee_service.dto.request.FeePreviewRequest;
import com.hdbank.customer_fee_service.dto.request.UpdateFeeConfigRequest;
import com.hdbank.customer_fee_service.dto.response.FeeConfigResponse;
import com.hdbank.customer_fee_service.dto.response.FeePreviewResponse;
import com.hdbank.customer_fee_service.entity.Customer;
import com.hdbank.customer_fee_service.entity.CustomerFeeConfig;
import com.hdbank.customer_fee_service.entity.FeeType;
import com.hdbank.customer_fee_service.exception.EntityNotFoundException;
import com.hdbank.customer_fee_service.exception.ValidationException;
import com.hdbank.customer_fee_service.repository.CustomerFeeConfigRepository;
import com.hdbank.customer_fee_service.repository.CustomerRepository;
import com.hdbank.customer_fee_service.repository.FeeTypeRepository;
import com.hdbank.customer_fee_service.service.strategy.FeeCalculationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeConfigService {

    private final CustomerFeeConfigRepository customerFeeConfigRepository;
    private final CustomerRepository customerRepository;
    private final FeeTypeRepository feeTypeRepository;
    private final FeeCalculationContext feeCalculationContext;

    @Transactional
    public FeeConfigResponse createFeeConfig(CreateFeeConfigRequest request) {
        log.info("Creating fee config for customerId: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .filter( c -> c.getDeletedAt() == null )
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));

        FeeType feeType = feeTypeRepository.findById(request.getFeeTypeId())
                .filter(FeeType::getIsActive)
                .orElseThrow(() -> new EntityNotFoundException("Fee type not found or inactive with id: " + request.getFeeTypeId()));

        if(request.getEffectiveTo() != null && request.getEffectiveFrom().isAfter(request.getEffectiveTo())){
            throw new ValidationException("Effective from date must be before effective to date");
        }

        checkOverlappingConfigs(request.getCustomerId(),
                request.getEffectiveFrom(),
                request.getEffectiveTo(),
                null); // because creates so excludeConfigId = null

        CustomerFeeConfig config = CustomerFeeConfig.builder()
                .customerId(request.getCustomerId())
                .feeTypeId(request.getFeeTypeId())
                .monthlyFeeAmount(request.getMonthlyFeeAmount())
                .currency(request.getCurrency())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .calculationParams(request.getCalculationParams())
                .build();

        CustomerFeeConfig saved = customerFeeConfigRepository.save(config);
        log.info("Created fee config with id: {}", saved.getId());
        return FeeConfigResponse.from(saved);
    }

    @Cacheable(value = "feeConfigs", key = "#configId")
    public FeeConfigResponse getFeeConfigById(Long configId) {
        log.info("Getting fee config with id: {}", configId);
        CustomerFeeConfig config = customerFeeConfigRepository.findById(configId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Fee config not found with id: " + configId));
        return FeeConfigResponse.from(config);
    }

    @Cacheable(value = "feeConfigs", key = "'customerId: ' + #customerId + ':active'")
    public FeeConfigResponse getActiveFeeConfigByCustomerId(Long customerId){
        log.info("Getting active fee config for customer id: {}", customerId);
        customerRepository.findById(customerId)
                .filter( c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        LocalDate today = LocalDate.now();
        CustomerFeeConfig config = customerFeeConfigRepository.findActiveConfigByCustomerIdAndDate(customerId,today)
                .orElseThrow(() -> new EntityNotFoundException("No active fee config found for customer id: " + customerId));
        return FeeConfigResponse.from(config);
    }

    @Cacheable(value = "feeConfigs", key = "'customerId: ' + #customerId + ':all'")
    public List<FeeConfigResponse> getAllFeeConfigsByCustomerIdIncludeExpired(Long customerId) {
        log.info("Getting all fee configs for customer id: {} including expired", customerId);
        customerRepository.findById(customerId)
                .filter( c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        List<CustomerFeeConfig> configs = customerFeeConfigRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
        if(configs.isEmpty()){
            throw new EntityNotFoundException("No fee configs found for customer id: " + customerId);
        }
        return configs.stream()
                .map(FeeConfigResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "feeConfigs", key = "#id")
    public FeeConfigResponse updateFeeConfig(Long id, UpdateFeeConfigRequest request){
        log.info("Updating fee config with id: {} ", id);

        CustomerFeeConfig config = customerFeeConfigRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Fee config not found with id: " + id));

        // Update fields if provided
        if(request.getMonthlyFeeAmount() != null) {
            config.setMonthlyFeeAmount(request.getMonthlyFeeAmount());
        }
        if(request.getCurrency() != null) {
            config.setCurrency(request.getCurrency());
        }
        if(request.getEffectiveFrom() != null) {
            config.setEffectiveFrom(request.getEffectiveFrom());
        }
        if(request.getEffectiveTo() != null) {
            config.setEffectiveTo(request.getEffectiveTo());
        }
        if(request.getCalculationParams() != null) {
            config.setCalculationParams(request.getCalculationParams());
        }

        // Validate dates after all updates applied
        if(config.getEffectiveTo() != null && config.getEffectiveFrom().isAfter(config.getEffectiveTo())){
            throw new ValidationException("Effective from date must be before effective to date");
        }
        checkOverlappingConfigs(
                config.getCustomerId(),
                config.getEffectiveFrom(),
                config.getEffectiveTo(),
                id);
        CustomerFeeConfig updated = customerFeeConfigRepository.save(config);
        log.info("Updated fee config with id: {}", updated.getId());
        return FeeConfigResponse.from(updated);
    }

    @Transactional
    @CacheEvict(value = "feeConfigs", key = "#id")
    public void deleteFeeConfig(Long id) {
        log.info("Deleting fee config with id: {}", id);

        CustomerFeeConfig config = customerFeeConfigRepository.findById(id)
                .filter( c -> c.getDeletedAt() == null )
                .orElseThrow(() -> new EntityNotFoundException("Fee config not found with id: " + id));

        config.setDeletedAt(Instant.now());
        config.setDeletedBy(0L); // System user

        customerFeeConfigRepository.save(config);
        log.info("Deleted fee config with id: {}", id);
    }

    public FeePreviewResponse feeReview(FeePreviewRequest request){
        log.info("Previewing fee for customer with id: {}",request.getCustomerId());
        LocalDate today = LocalDate.now();
        CustomerFeeConfig config = customerFeeConfigRepository
                .findActiveConfigByCustomerIdAndDate(request.getCustomerId(), today)
                .orElseThrow(
                        () -> new EntityNotFoundException("No active config found with id: " + request.getCustomerId())
                );

        FeeType feeType = feeTypeRepository
                .findById(config.getFeeTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Fee type not found"));

        // merge params: use request param if provide or otherwise use use config params
        Map<String, Object> calculationParams = request.getCalculationParams() != null
                ? request.getCalculationParams()
                : config.getCalculationParams();

        BigDecimal calculationFee = feeCalculationContext.calculateFee(
                feeType.getCalculationType(),
                config.getMonthlyFeeAmount(),
                calculationParams
        );

        return FeePreviewResponse.builder()
                .customerId(request.getCustomerId())
                .feeTypeCode(feeType.getCode())
                .feeTypeName(feeType.getName())
                .calculationType(feeType.getCalculationType())
                .monthlyFeeAmount(config.getMonthlyFeeAmount())
                .calculatedFee(calculationFee)
                .currency(config.getCurrency())
                .calculationParams(calculationParams)
                .build();
    }

    /**
     * Checking overlapping configs
     * Ensure customer only has 1 active config at a time
     */
    private void checkOverlappingConfigs(
            Long customerId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Long excludeConfigId) {
        List<CustomerFeeConfig> existingConfigs = customerFeeConfigRepository.findByCustomerIdAndDeletedAtIsNull(customerId);

        for (CustomerFeeConfig existing : existingConfigs) {
            // Skip nếu đang check chính config này (khi update) -> nếu không sẽ luôn overlap với chính nó
            if (existing.getId().equals(excludeConfigId)) {
                continue;
            }

            LocalDate existingFrom = existing.getEffectiveFrom();
            LocalDate existingTo = existing.getEffectiveTo();

            boolean hasOverlap = isDateRangeOverlap(
                    effectiveFrom, effectiveTo, // new config range
                    existingFrom, existingTo); // existing config range

            if (hasOverlap) {
                throw new ValidationException(
                        String.format("Fee config overlaps with existing config (id: %d) for this customer", existing.getId())
                );
            }
        }
    }

    private boolean isDateRangeOverlap(
            LocalDate from1,
            LocalDate to1,
            LocalDate from2,
            LocalDate to2) {

        LocalDate end1 = (to1 != null) ? to1 : LocalDate.MAX;
        LocalDate end2 = (to2 != null) ? to2 : LocalDate.MAX;

        // Overlap nếu: start1 <= end2 AND start2 <= end1
        return !from1.isAfter(end2) && !from2.isAfter(end1);
    }
}
