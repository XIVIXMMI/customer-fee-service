package com.hdbank.customer_fee_service;

import com.hdbank.customer_fee_service.kafka.FeeChargedConsumer;
import com.hdbank.customer_fee_service.kafka.FeeChargedDLQConsumer;
import com.hdbank.customer_fee_service.kafka.FeeChargedProducer;
import com.hdbank.customer_fee_service.kafka.FeeChargedRetryConsumer;
import com.hdbank.customer_fee_service.scheduler.DistributedLockService;
import com.hdbank.customer_fee_service.scheduler.FeeJobExecuteScheduler;
import com.hdbank.customer_fee_service.scheduler.FeeJobPrepareScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CustomerFeeServiceApplicationTests {

	// Mock Kafka beans - FIXED: Should be KafkaTemplate<String, String>
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

	// Mock Scheduler beans
	@MockitoBean
	private FeeJobPrepareScheduler feeJobPrepareScheduler;

	@MockitoBean
	private FeeJobExecuteScheduler feeJobExecuteScheduler;

	@MockitoBean
	private DistributedLockService distributedLockService;

	@Test
	void contextLoads() {
		// Test that Spring context loads successfully
	}

}
