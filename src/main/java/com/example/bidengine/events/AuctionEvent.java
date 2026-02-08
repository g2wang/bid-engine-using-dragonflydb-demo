package com.example.bidengine.events;

public record AuctionEvent(
        String eventId,
        String eventType,
        String auctionId,
        long occurredAtEpochMs,
        String sellerId,
        String title,
        String description,
        Long startingPrice,
        Long reservePrice,
        Long startTimeEpochMs,
        Long endTimeEpochMs,
        String bidId,
        String bidderId,
        Long amount,
        String status
) {
}
