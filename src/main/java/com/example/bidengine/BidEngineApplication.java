package com.example.bidengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BidEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(BidEngineApplication.class, args);
    }
}
