package com.example.bidengine.service;

import com.example.bidengine.api.*;
import com.example.bidengine.events.AuctionEvent;
import com.example.bidengine.redis.AuctionRedisRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuctionService {
    private final AuctionRedisRepository redisRepository;
    private final KafkaTemplate<String, AuctionEvent> kafkaTemplate;
    private final String topic;

    public AuctionService(AuctionRedisRepository redisRepository,
                          KafkaTemplate<String, AuctionEvent> kafkaTemplate,
                          @Value("${bidengine.kafka.topic}") String topic) {
        this.redisRepository = redisRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public AuctionResponse createAuction(CreateAuctionRequest request) {
        if (request.endTimeEpochMs() <= request.startTimeEpochMs()) {
            throw new IllegalArgumentException("endTimeEpochMs must be greater than startTimeEpochMs");
        }
        long now = Instant.now().toEpochMilli();
        String auctionId = UUID.randomUUID().toString();
        AuctionResponse auction = new AuctionResponse(
                auctionId,
                request.sellerId(),
                request.title(),
                request.description(),
                "OPEN",
                request.startingPrice(),
                request.reservePrice(),
                request.startTimeEpochMs(),
                request.endTimeEpochMs(),
                null,
                null,
                now,
                now
        );
        redisRepository.createAuction(auction);
        kafkaTemplate.send(topic, auctionId, new AuctionEvent(
                UUID.randomUUID().toString(),
                "AUCTION_CREATED",
                auctionId,
                now,
                auction.sellerId(),
                auction.title(),
                auction.description(),
                auction.startingPrice(),
                auction.reservePrice(),
                auction.startTimeEpochMs(),
                auction.endTimeEpochMs(),
                null,
                null,
                null,
                auction.status()
        ));
        return auction;
    }

    public AuctionResponse getAuction(String auctionId) {
        return redisRepository.getAuction(auctionId);
    }

    public PlaceBidResponse placeBid(String auctionId, PlaceBidRequest request) {
        String bidId = UUID.randomUUID().toString();
        long now = Instant.now().toEpochMilli();
        var result = redisRepository.placeBid(auctionId, bidId, request.bidderId(), request.amount(), now);
        if (!result.ok()) {
            return new PlaceBidResponse(bidId, auctionId, result.errorCode(), null, null);
        }
        kafkaTemplate.send(topic, auctionId, new AuctionEvent(
                UUID.randomUUID().toString(),
                "BID_PLACED",
                auctionId,
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                bidId,
                request.bidderId(),
                request.amount(),
                "OPEN"
        ));
        return new PlaceBidResponse(bidId, auctionId, "OK", result.highestBid(), result.highestBidderId());
    }

    public CloseAuctionResponse closeAuction(String auctionId) {
        long now = Instant.now().toEpochMilli();
        var result = redisRepository.closeAuction(auctionId, now);
        if (!result.ok()) {
            return new CloseAuctionResponse(auctionId, result.errorCode(), null, null);
        }
        String status = result.status();
        Long winningBid = "CLOSED".equals(status) ? result.highestBid() : null;
        String winningBidderId = "CLOSED".equals(status) ? result.highestBidderId() : null;
        kafkaTemplate.send(topic, auctionId, new AuctionEvent(
                UUID.randomUUID().toString(),
                "AUCTION_CLOSED",
                auctionId,
                now,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                winningBidderId,
                winningBid,
                status
        ));
        return new CloseAuctionResponse(auctionId, status, winningBid, winningBidderId);
    }

    public List<BidResponse> listTopBids(String auctionId, int limit) {
        return redisRepository.listTopBids(auctionId, limit);
    }

    public void autoCloseExpiredAuctions(int limit) {
        long now = Instant.now().toEpochMilli();
        List<String> expired = redisRepository.listAuctionsEndingBefore(now, limit);
        for (String auctionId : expired) {
            closeAuction(auctionId);
            redisRepository.removeFromSchedule(auctionId);
        }
    }
}
