package com.hdbank.customer_fee_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customeOpenApi(){
        return new OpenAPI()
                .info( new Info()
                        .title("Customer Fee Service")
                        .version("1.0.0")
                        .description("""
                                REST API for Customer Fee Service\s
                               \s
                                ##FEATURES
                                - Customer Management\s
                                - Fee Type Management
                                - Fee Preview & Calculation\s
                                - Automated Fee Charging (Scheduler)
                                - Distributed Cache (Redis)
                                - Event-Driven Architecture (Kafka)
                               \s
                                ##BUSSINESS LOGIC
                                - Support 3 fee calculation strategies:
                                    - Fixed Monthly Fee
                                    - Tiered Balance Fee
                                    - Percentage of Balance Fee
                                - Monthly automated fee charging
                                - Distributed lock for scheduler coordination
                               \s""")
                        .contact(new Contact()
                                .name("HDBank Development Team"))
                        .license(new License()
                                .name("Internal use only"))
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server")
                ));
    }
}
