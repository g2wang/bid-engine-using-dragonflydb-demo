package com.example.bidengine.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAuctionRequest(
        @NotBlank String sellerId,
        @NotBlank String title,
        String description,
        @NotNull @PositiveOrZero Long startingPrice,
        @PositiveOrZero Long reservePrice,
        @NotNull Long startTimeEpochMs,
        @NotNull Long endTimeEpochMs
) {
}
