package com.example.bidengine.api;

import com.example.bidengine.service.AuctionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auctions")
public class AuctionController {
    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(@Valid @RequestBody CreateAuctionRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(auctionService.createAuction(request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionResponse> getAuction(@PathVariable String auctionId) {
        AuctionResponse response = auctionService.getAuction(auctionId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<PlaceBidResponse> placeBid(@PathVariable String auctionId,
                                                     @Valid @RequestBody PlaceBidRequest request) {
        PlaceBidResponse response = auctionService.placeBid(auctionId, request);
        return switch (response.status()) {
            case "OK" -> ResponseEntity.ok(response);
            case "NOT_FOUND" -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            case "NOT_OPEN", "NOT_STARTED", "ENDED", "BELOW_START", "BELOW_HIGHEST" ->
                    ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        };
    }

    @PostMapping("/{auctionId}/close")
    public ResponseEntity<CloseAuctionResponse> closeAuction(@PathVariable String auctionId) {
        CloseAuctionResponse response = auctionService.closeAuction(auctionId);
        return switch (response.status()) {
            case "CLOSED", "CLOSED_NO_SALE" -> ResponseEntity.ok(response);
            case "NOT_FOUND" -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            case "NOT_OPEN" -> ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        };
    }

    @GetMapping("/{auctionId}/bids")
    public ResponseEntity<List<BidResponse>> listBids(@PathVariable String auctionId,
                                                      @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auctionService.listTopBids(auctionId, limit));
    }
}
