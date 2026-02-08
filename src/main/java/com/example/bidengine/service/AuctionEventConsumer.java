package com.example.bidengine.service;

import com.example.bidengine.events.AuctionEvent;
import com.example.bidengine.persistence.AuctionEntity;
import com.example.bidengine.persistence.AuctionRepository;
import com.example.bidengine.persistence.BidEntity;
import com.example.bidengine.persistence.BidRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuctionEventConsumer {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public AuctionEventConsumer(AuctionRepository auctionRepository, BidRepository bidRepository) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
    }

    @KafkaListener(topics = "${bidengine.kafka.topic}")
    public void handle(AuctionEvent event) {
        if (event == null || event.eventType() == null) {
            return;
        }
        switch (event.eventType()) {
            case "AUCTION_CREATED" -> handleAuctionCreated(event);
            case "BID_PLACED" -> handleBidPlaced(event);
            case "AUCTION_CLOSED" -> handleAuctionClosed(event);
            default -> {
            }
        }
    }

    private void handleAuctionCreated(AuctionEvent event) {
        AuctionEntity entity = auctionRepository.findById(event.auctionId())
                .orElseGet(() -> new AuctionEntity(event.auctionId()));
        entity.setSellerId(event.sellerId());
        entity.setTitle(event.title());
        entity.setDescription(event.description());
        entity.setStatus(event.status());
        entity.setStartingPrice(event.startingPrice());
        entity.setReservePrice(event.reservePrice());
        entity.setStartTimeEpochMs(event.startTimeEpochMs());
        entity.setEndTimeEpochMs(event.endTimeEpochMs());
        entity.setCreatedAtEpochMs(event.occurredAtEpochMs());
        entity.setUpdatedAtEpochMs(event.occurredAtEpochMs());
        auctionRepository.save(entity);
    }

    private void handleBidPlaced(AuctionEvent event) {
        if (event.bidId() != null) {
            BidEntity bid = new BidEntity(event.bidId());
            bid.setAuctionId(event.auctionId());
            bid.setBidderId(event.bidderId());
            bid.setAmount(event.amount());
            bid.setPlacedAtEpochMs(event.occurredAtEpochMs());
            bidRepository.save(bid);
        }
        auctionRepository.findById(event.auctionId()).ifPresent(entity -> {
            entity.setHighestBid(event.amount());
            entity.setHighestBidderId(event.bidderId());
            entity.setUpdatedAtEpochMs(event.occurredAtEpochMs());
            auctionRepository.save(entity);
        });
    }

    private void handleAuctionClosed(AuctionEvent event) {
        auctionRepository.findById(event.auctionId()).ifPresent(entity -> {
            entity.setStatus(event.status() == null ? "CLOSED" : event.status());
            if (event.amount() != null) {
                entity.setHighestBid(event.amount());
                entity.setHighestBidderId(event.bidderId());
            } else {
                entity.setHighestBid(null);
                entity.setHighestBidderId(null);
            }
            entity.setUpdatedAtEpochMs(event.occurredAtEpochMs());
            auctionRepository.save(entity);
        });
    }
}
