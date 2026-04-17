package com.lokesh.ratelimiter.sample;

import com.lokesh.ratelimiter.core.model.RateLimitResult;
import com.lokesh.ratelimiter.core.port.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SampleController {

    private final RateLimiter rateLimiter;

    public SampleController(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/limited")
    public ResponseEntity<String> getLimitedResource() {
        // 1. Call the domain core to see if this request is allowed.
        // We use a hardcoded user "test_user" and the "benchmark" plan for the load test.
        RateLimitResult result = rateLimiter.allow("test_user", List.of("benchmark"), 1);

        // 2. Return HTTP 200 if allowed, HTTP 429 if denied.
        if (result.allowed()) {
            return ResponseEntity.ok("Success. Remaining tokens: " + result.remainingTokens());
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate Limited. Wait millis: " + result.waitMillis());
        }
    }
}