package com.hdbank.customer_fee_service.scheduler;

import com.hdbank.customer_fee_service.entity.Customer;
import com.hdbank.customer_fee_service.entity.CustomerFeeJob;
import com.hdbank.customer_fee_service.entity.FeeJobStatus;
import com.hdbank.customer_fee_service.repository.CustomerFeeJobRepository;
import com.hdbank.customer_fee_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduler to prepare fee jobs for all active customers
 * Runs monthly on 1st day of month at 00:01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeeJobPrepareScheduler {

    private final CustomerRepository customerRepository;
    private final CustomerFeeJobRepository feeJobRepository;
    private final DistributedLockService lockService;

    private static final String LOCK_KEY = "FEE_JOB_PREPARE_SCHEDULER";

    @Scheduled(cron = "0 1 0 1 * ?")  // 00:01 on day 1 of every month and any day in a week
    public void prepareMonthlyFeeJobs() {
        log.info("Starting FeeJobPrepareScheduler...");

        lockService.executeWithLock(LOCK_KEY, this::executePrepareJobs);
    }

    /**
     * For testing - runs every 2 minutes
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void prepareMonthlyFeeJobsTest() {
        log.info("Starting FeeJobPrepareScheduler (TEST MODE)...");
        lockService.executeWithLock(LOCK_KEY + "_TEST", this::executePrepareJobs);
    }

    private void executePrepareJobs() {
        String billingMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("Preparing fee jobs for billing month: {}", billingMonth);

        List<Customer> activeCustomers = customerRepository
                .findByStatusAndDeletedAtIsNull("ACTIVE");
        log.info("Found {} active customers", activeCustomers.size());

        int created = 0;
        int skipped = 0;

        for (Customer customer : activeCustomers) {
            try {
                // Check if a job already exists (idempotency)
                String idempotencyKey = customer.getId() + "_" + billingMonth;
                boolean exists = feeJobRepository.existsByIdempotencyKey(idempotencyKey);

                if (exists) {
                    log.debug("Job already exists for customer {} in month {}",
                            customer.getId(), billingMonth);
                    skipped++;
                    continue;
                }

                CustomerFeeJob job = CustomerFeeJob.builder()
                        .customerId(customer.getId())
                        .billingMonth(billingMonth)
                        .status(FeeJobStatus.NEW)
                        .idempotencyKey(idempotencyKey)
                        .build();

                feeJobRepository.save(job);
                created++;

                log.info("Created fee job for customer {}: job id {}",
                        customer.getId(), job.getId());

            } catch (Exception e) {
                log.error("Error creating job for customer {}", customer.getId(), e);
            }
        }

        log.info("Fee job preparation completed. Created: {}, Skipped: {}", created, skipped);
    }
}