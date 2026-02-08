package com.example.bidengine.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaceBidRequest(
        @NotBlank String bidderId,
        @NotNull @Positive Long amount
) {
}
