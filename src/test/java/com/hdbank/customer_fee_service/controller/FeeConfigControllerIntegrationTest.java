package com.hdbank.customer_fee_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdbank.customer_fee_service.dto.request.CreateFeeConfigRequest;
import com.hdbank.customer_fee_service.entity.Customer;
import com.hdbank.customer_fee_service.entity.CustomerFeeConfig;
import com.hdbank.customer_fee_service.entity.FeeType;
import com.hdbank.customer_fee_service.kafka.FeeChargedConsumer;
import com.hdbank.customer_fee_service.kafka.FeeChargedDLQConsumer;
import com.hdbank.customer_fee_service.kafka.FeeChargedProducer;
import com.hdbank.customer_fee_service.kafka.FeeChargedRetryConsumer;
import com.hdbank.customer_fee_service.repository.CustomerFeeConfigRepository;
import com.hdbank.customer_fee_service.repository.CustomerRepository;
import com.hdbank.customer_fee_service.repository.FeeTypeRepository;
import com.hdbank.customer_fee_service.scheduler.DistributedLockService;
import com.hdbank.customer_fee_service.scheduler.FeeJobExecuteScheduler;
import com.hdbank.customer_fee_service.scheduler.FeeJobPrepareScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeeConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FeeTypeRepository feeTypeRepository;

    @Autowired
    private CustomerFeeConfigRepository feeConfigRepository;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private FeeChargedProducer feeChargedProducer;

    @MockitoBean
    private FeeChargedConsumer feeChargedConsumer;

    @MockitoBean
    private FeeChargedRetryConsumer feeChargedRetryConsumer;

    @MockitoBean
    private FeeChargedDLQConsumer feeChargedDLQConsumer;

    @MockitoBean
    private FeeJobPrepareScheduler feeJobPrepareScheduler;

    @MockitoBean
    private FeeJobExecuteScheduler feeJobExecuteScheduler;

    @MockitoBean
    private DistributedLockService distributedLockService;

    private Customer testCustomer;
    private FeeType testFeeType;

    @BeforeEach
    void setUp() {
        feeConfigRepository.deleteAll();
        customerRepository.deleteAll();
        feeTypeRepository.deleteAll();

        testCustomer = Customer.builder()
                .fullName("Test Customer")
                .email("test@example.com")
                .phoneNumber("0123456789")
                .status("ACTIVE")
                .build();
        testCustomer = customerRepository.save(testCustomer);

        testFeeType = FeeType.builder()
                .code("FIXED_MONTHLY")
                .name("Fixed Monthly Fee")
                .description("Fixed monthly fee")
                .calculationType("FIXED")
                .isActive(true)
                .build();
        testFeeType = feeTypeRepository.save(testFeeType);
    }

    @Test
    void shouldCreateFeeConfigSuccessfully() throws Exception {
        CreateFeeConfigRequest request = new CreateFeeConfigRequest();
        request.setCustomerId(testCustomer.getId());
        request.setFeeTypeId(testFeeType.getId());
        request.setMonthlyFeeAmount(new BigDecimal("50000"));
        request.setCurrency("VND");
        request.setEffectiveFrom(LocalDate.now());
        request.setEffectiveTo(LocalDate.now().plusMonths(6));
        request.setCalculationParams(new HashMap<>());

        mockMvc.perform(post("/api/v1/fee-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.response_code", is("00")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.customer_id", is(testCustomer.getId().intValue())))
                .andExpect(jsonPath("$.data.monthly_fee_amount", is(50000)))
                .andExpect(jsonPath("$.data.currency", is("VND")));
    }

    @Test
    void shouldRejectOverlappingFeeConfigs() throws Exception {
        CustomerFeeConfig existingConfig = CustomerFeeConfig.builder()
                .customerId(testCustomer.getId())
                .feeTypeId(testFeeType.getId())
                .monthlyFeeAmount(new BigDecimal("30000"))
                .currency("VND")
                .effectiveFrom(LocalDate.now())
                .effectiveTo(LocalDate.now().plusMonths(3))
                .calculationParams(new HashMap<>())
                .build();
        feeConfigRepository.save(existingConfig);

        CreateFeeConfigRequest overlappingRequest = new CreateFeeConfigRequest();
        overlappingRequest.setCustomerId(testCustomer.getId());
        overlappingRequest.setFeeTypeId(testFeeType.getId());
        overlappingRequest.setMonthlyFeeAmount(new BigDecimal("50000"));
        overlappingRequest.setCurrency("VND");
        overlappingRequest.setEffectiveFrom(LocalDate.now().plusMonths(1));
        overlappingRequest.setEffectiveTo(LocalDate.now().plusMonths(6));
        overlappingRequest.setCalculationParams(new HashMap<>());

        mockMvc.perform(post("/api/v1/fee-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.response_code", is("01")));
    }

    @Test
    void shouldGetActiveFeeConfigByCustomerId() throws Exception {
        CustomerFeeConfig activeConfig = CustomerFeeConfig.builder()
                .customerId(testCustomer.getId())
                .feeTypeId(testFeeType.getId())
                .monthlyFeeAmount(new BigDecimal("40000"))
                .currency("VND")
                .effectiveFrom(LocalDate.now().minusMonths(1))
                .effectiveTo(LocalDate.now().plusMonths(2))
                .calculationParams(new HashMap<>())
                .build();
        feeConfigRepository.save(activeConfig);

        mockMvc.perform(get("/api/v1/fee-configs/customer/" + testCustomer.getId() + "/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_code", is("00")))
                .andExpect(jsonPath("$.data.customer_id", is(testCustomer.getId().intValue())))
                .andExpect(jsonPath("$.data.monthly_fee_amount", is(40000)));
    }

    @Test
    void shouldReturnNotFoundWhenNoActiveFeeConfig() throws Exception {
        mockMvc.perform(get("/api/v1/fee-configs/customer/" + testCustomer.getId() + "/active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.response_code", is("02")));
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        CreateFeeConfigRequest request = new CreateFeeConfigRequest();
        request.setCustomerId(testCustomer.getId());
        request.setFeeTypeId(testFeeType.getId());
        request.setMonthlyFeeAmount(new BigDecimal("50000"));
        request.setCurrency("VND");
        request.setEffectiveFrom(LocalDate.now().plusMonths(3));
        request.setEffectiveTo(LocalDate.now());
        request.setCalculationParams(new HashMap<>());

        mockMvc.perform(post("/api/v1/fee-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.response_code", is("01")));
    }
}
