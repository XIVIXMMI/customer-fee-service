package com.hdbank.customer_fee_service.service;

import com.hdbank.customer_fee_service.dto.response.FeeConfigResponse;
import com.hdbank.customer_fee_service.dto.response.FeeTypeResponse;
import com.hdbank.customer_fee_service.entity.FeeType;
import com.hdbank.customer_fee_service.repository.FeeTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeTypeService {

    private final FeeTypeRepository feeTypeRepository;

    @Cacheable(value = "feeTypes", key = "#feeTypeId")
    public FeeTypeResponse getFeeTypeById(Long feeTypeId) {
        log.info("Getting FeeType with ID: {}", feeTypeId);
        FeeType feeType = feeTypeRepository.findById(feeTypeId)
                .filter(FeeType::getIsActive)
                .orElseThrow(() -> new RuntimeException("FeeType not found with id: " + feeTypeId));
        return FeeTypeResponse.from(feeType);
    }

    @Cacheable(value = "feeTypes", key = "'allActive'")
    public List<FeeTypeResponse> getAllFeeTypeActive(){
        log.info("Getting all Fee Types");
        List<FeeType> feeType = feeTypeRepository.findByIsActiveTrue();
        return feeType.stream()
                .map(FeeTypeResponse::from)
                .collect(Collectors.toList());
    }
}
