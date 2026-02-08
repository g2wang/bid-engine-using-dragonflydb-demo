package com.example.bidengine.api;

public record PlaceBidResponse(
        String bidId,
        String auctionId,
        String status,
        Long highestBid,
        String highestBidderId
) {
}
