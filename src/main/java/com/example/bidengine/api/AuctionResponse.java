package com.example.bidengine.api;

public record AuctionResponse(
        String auctionId,
        String sellerId,
        String title,
        String description,
        String status,
        long startingPrice,
        Long reservePrice,
        long startTimeEpochMs,
        long endTimeEpochMs,
        Long highestBid,
        String highestBidderId,
        long createdAtEpochMs,
        long updatedAtEpochMs
) {
}
