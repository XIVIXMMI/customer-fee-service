package com.hdbank.customer_fee_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
//@EnableScheduling
//@EnableCaching
@EnableRetry
public class CustomerFeeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerFeeServiceApplication.class, args);
	}

}
