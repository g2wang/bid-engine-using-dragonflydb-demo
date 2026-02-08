package com.example.bidengine.api;

public record CloseAuctionResponse(
        String auctionId,
        String status,
        Long winningBid,
        String winningBidderId
) {
}
