package com.example.bidengine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionAutoCloseScheduler {
    private final AuctionService auctionService;
    private final int batchLimit;

    public AuctionAutoCloseScheduler(AuctionService auctionService,
                                     @Value("${bidengine.scheduler.close-batch-limit:100}") int batchLimit) {
        this.auctionService = auctionService;
        this.batchLimit = batchLimit;
    }

    @Scheduled(fixedDelayString = "${bidengine.scheduler.close-delay-ms:1000}")
    public void closeExpiredAuctions() {
        auctionService.autoCloseExpiredAuctions(batchLimit);
    }
}
