package com.hdbank.customer_fee_service.service;

import com.hdbank.customer_fee_service.dto.request.CreateCustomerRequest;
import com.hdbank.customer_fee_service.dto.request.UpdateCustomerRequest;
import com.hdbank.customer_fee_service.dto.response.CustomerResponse;
import com.hdbank.customer_fee_service.entity.Customer;
import com.hdbank.customer_fee_service.exception.EntityNotFoundException;
import com.hdbank.customer_fee_service.exception.ValidationException;
import com.hdbank.customer_fee_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerResponse createCustomer(CreateCustomerRequest request){
        log.info("Creating customer with email: {}", request.getEmail());

        if(customerRepository.existsByEmail(request.getEmail())){
            throw new ValidationException("Email already in use " + request.getEmail());
        }

        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .status(request.getStatus())
                .build();

        Customer saved = customerRepository.save(customer);
        log.info("Customer created with id: {}", saved.getId());
        return CustomerResponse.from(saved);
    }

    public CustomerResponse getCustomerById(Long customerId){
        log.info("Getting customer with id: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        if(customer.getDeletedAt() != null){
            throw new EntityNotFoundException("Customer is deleted with id: " + customerId);
        }
        return CustomerResponse.from(customer);
    }

    public Page<CustomerResponse> getAllCustomers(Pageable pageable){
        log.info("Getting customers page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Customer> customers = customerRepository.findByDeletedAtIsNull(pageable);
        return customers.map(CustomerResponse::from);
    }

    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request){
        log.info("Updating customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));

        if(request.getEmail() != null && !request.getEmail().equals(customer.getEmail())){
            if(customerRepository.existsByEmail(request.getEmail())){
                throw new ValidationException("Email already in use " + request.getEmail());
            }
            customer.setEmail(request.getEmail());
        }

        if(request.getFullName() != null){
            customer.setFullName(request.getFullName());
        }
        if(request.getPhoneNumber() != null){
            customer.setPhoneNumber(request.getPhoneNumber());
        }
        if(request.getStatus() != null){
            customer.setStatus(request.getStatus());
        }

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated with id: {}", updated.getId());
        return CustomerResponse.from(updated);
    }

    public void deleteCustomer(Long id) {
        log.info("Deleting customer with id: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + id));

        customer.setDeletedAt(Instant.now());
        customer.setDeletedBy(0L); // System user will be replaced with actual user id in the future

        customerRepository.save(customer);
        log.info("Deleted customer with id: {}", id);
    }
}
