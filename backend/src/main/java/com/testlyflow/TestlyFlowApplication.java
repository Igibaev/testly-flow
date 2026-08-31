package com.testlyflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TestlyFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestlyFlowApplication.class, args);
    }
}
