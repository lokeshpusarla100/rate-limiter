package com.lokesh.ratelimiter.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Rate Limiter Sample Application.
 * 
 * This application demonstrates the integration between the Core Domain 
 * and the Redis Infrastructure module. It is used for local verification 
 * and performance benchmarking (load testing).
 */
@SpringBootApplication
public class SampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}