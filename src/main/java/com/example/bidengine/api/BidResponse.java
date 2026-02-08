package com.example.bidengine.api;

public record BidResponse(
        String bidId,
        String auctionId,
        String bidderId,
        long amount,
        long placedAtEpochMs
) {
}
