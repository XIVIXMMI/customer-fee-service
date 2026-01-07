package com.hdbank.customer_fee_service.service;

import com.hdbank.customer_fee_service.dto.request.CreateCustomerRequest;
import com.hdbank.customer_fee_service.dto.response.CustomerResponse;
import com.hdbank.customer_fee_service.entity.Customer;
import com.hdbank.customer_fee_service.exception.EntityNotFoundException;
import com.hdbank.customer_fee_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer mockCustomer;

    @BeforeEach
    void setUp() {
        mockCustomer = Customer.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("nguyenvana@example.com")
                .phoneNumber("0123456789")
                .status("ACTIVE")
                .build();
    }

    @Test
    void shouldCreateCustomerSuccessfully() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("nguyenvana@example.com");
        request.setPhoneNumber("0123456789");
        request.setStatus("ACTIVE");

        when(customerRepository.save(any(Customer.class))).thenReturn(mockCustomer);

        CustomerResponse response = customerService.createCustomer(request);

        assertNotNull(response);
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals("nguyenvana@example.com", response.getEmail());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        when(customerRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
            () -> customerService.getCustomerById(999L));
    }

    @Test
    void shouldSoftDeleteCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(mockCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(mockCustomer);

        customerService.deleteCustomer(1L);

        verify(customerRepository).save(any(Customer.class));
        assertNotNull(mockCustomer.getDeletedAt());
    }
}
