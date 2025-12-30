package com.hdbank.customer_fee_service;

import com.hdbank.customer_fee_service.kafka.FeeChargedEvent;
import com.hdbank.customer_fee_service.kafka.FeeChargedProducer;
import com.hdbank.customer_fee_service.scheduler.DistributedLockService;
import com.hdbank.customer_fee_service.scheduler.FeeJobExecuteScheduler;
import com.hdbank.customer_fee_service.scheduler.FeeJobPrepareScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootTest
class CustomerFeeServiceApplicationTests {

	// Mock Kafka beans
	@MockBean
	private KafkaTemplate<String, FeeChargedEvent> kafkaTemplate;

	@MockBean
	private FeeChargedProducer feeChargedProducer;

	// Mock Scheduler beans
	@MockBean
	private FeeJobPrepareScheduler feeJobPrepareScheduler;

	@MockBean
	private FeeJobExecuteScheduler feeJobExecuteScheduler;

	@MockBean
	private DistributedLockService distributedLockService;

	@Test
	void contextLoads() {
	}

}
