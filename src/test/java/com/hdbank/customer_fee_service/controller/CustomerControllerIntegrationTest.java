package com.hdbank.customer_fee_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdbank.customer_fee_service.dto.request.CreateCustomerRequest;
import com.hdbank.customer_fee_service.dto.request.UpdateCustomerRequest;
import com.hdbank.customer_fee_service.entity.Customer;
import com.hdbank.customer_fee_service.kafka.FeeChargedConsumer;
import com.hdbank.customer_fee_service.kafka.FeeChargedDLQConsumer;
import com.hdbank.customer_fee_service.kafka.FeeChargedProducer;
import com.hdbank.customer_fee_service.kafka.FeeChargedRetryConsumer;
import com.hdbank.customer_fee_service.repository.CustomerRepository;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

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

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveCustomer() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("nguyenvana@test.com");
        request.setPhoneNumber("0123456789");
        request.setStatus("ACTIVE");

        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.response_code", is("00")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.full_name", is("Nguyen Van A")))
                .andExpect(jsonPath("$.data.email", is("nguyenvana@test.com")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long customerId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_code", is("00")))
                .andExpect(jsonPath("$.data.id", is(customerId.intValue())))
                .andExpect(jsonPath("$.data.full_name", is("Nguyen Van A")))
                .andExpect(jsonPath("$.data.email", is("nguyenvana@test.com")));
    }

    @Test
    void shouldUpdateCustomer() throws Exception {
        Customer customer = Customer.builder()
                .fullName("Nguyen Van B")
                .email("nguyenvanb@test.com")
                .phoneNumber("0987654321")
                .status("ACTIVE")
                .build();
        customer = customerRepository.save(customer);

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setFullName("Nguyen Van B Updated");
        request.setEmail("updated@test.com");

        mockMvc.perform(put("/api/v1/customers/" + customer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_code", is("00")))
                .andExpect(jsonPath("$.data.full_name", is("Nguyen Van B Updated")))
                .andExpect(jsonPath("$.data.email", is("updated@test.com")));
    }

    @Test
    void shouldSoftDeleteCustomer() throws Exception {
        Customer customer = Customer.builder()
                .fullName("Nguyen Van C")
                .email("nguyenvanc@test.com")
                .phoneNumber("0111222333")
                .status("ACTIVE")
                .build();
        customer = customerRepository.save(customer);

        mockMvc.perform(delete("/api/v1/customers/" + customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_code", is("00")));

        mockMvc.perform(get("/api/v1/customers/" + customer.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForNonExistentCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/customers/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.response_code", is("02")));
    }
}
