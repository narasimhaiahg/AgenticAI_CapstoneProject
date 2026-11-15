package com.capstone.fraudcase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FraudInvestigationApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudInvestigationApplication.class, args);
    }
}
