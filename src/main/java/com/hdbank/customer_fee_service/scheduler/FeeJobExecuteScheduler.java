package com.hdbank.customer_fee_service.scheduler;

import com.hdbank.customer_fee_service.dto.response.FeeChargeResult;
import com.hdbank.customer_fee_service.entity.CustomerFeeJob;
import com.hdbank.customer_fee_service.entity.FeeJobStatus;
import com.hdbank.customer_fee_service.kafka.FeeChargedProducer;
import com.hdbank.customer_fee_service.repository.CustomerFeeJobRepository;
import com.hdbank.customer_fee_service.service.FeeChargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler to execute pending fee jobs
 * Runs every hour to process jobs with status = NEW
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeeJobExecuteScheduler {

    private final CustomerFeeJobRepository feeJobRepository;
    private final FeeChargeService feeChargeService;
    private final FeeChargedProducer kafkaProducer;
    private final DistributedLockService lockService;

    private static final String LOCK_KEY = "FEE_JOB_EXECUTE_SCHEDULER";
    private static final int BATCH_SIZE = 100;  // Process 100 jobs per run

    /**
     * Runs every hour at minute 0
     */
    @Scheduled(cron = "0 0 * * * ?")  // Every hour at :00
    public void executeNewFeeJobs() {
        log.info("Starting FeeJobExecuteScheduler...");

        lockService.executeWithLock(LOCK_KEY, this::executeJobs);
    }

    /**
     * For testing - runs every 2 minutes
     */
    @Scheduled(cron = "0 */1 * * * ?")  // Every 2 minutes
    public void executeNewFeeJobsTest() {
        log.info("Starting FeeJobExecuteScheduler (TEST MODE)...");
        lockService.executeWithLock(LOCK_KEY + "_TEST", this::executeJobs);
    }

    private void executeJobs() {
        // Get NEW jobs (limit batch size to avoid overload)
        List<CustomerFeeJob> newJobs = feeJobRepository.findByStatus(
                FeeJobStatus.NEW, PageRequest.of(0,BATCH_SIZE) // limit 100 jobs in a time
        );

        if (newJobs.isEmpty()) {
            log.info("No NEW jobs to process");
            return;
        }

        log.info("Found {} NEW jobs to process", newJobs.size());

        int success = 0;
        int failed = 0;

        for (CustomerFeeJob job : newJobs) {
            try {
                log.info("Processing job: {} for customer: {}",
                        job.getId(), job.getCustomerId());

                // Charge fee
                FeeChargeResult result = feeChargeService.chargeFee(job.getId());

                // Publish to Kafka (only if success)
                if ("SUCCESS".equals(result.getStatus())) {
                    kafkaProducer.publishFeeChargedEvent(result);
                    success++;
                } else {
                    failed++;
                }

            } catch (Exception e) {
                log.error("Error processing job: {}", job.getId(), e);
                failed++;
            }
        }

        log.info("Job execution completed. Success: {}, Failed: {}", success, failed);
    }
}