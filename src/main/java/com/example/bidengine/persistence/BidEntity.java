package com.example.bidengine.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bids")
public class BidEntity {
    @Id
    private String id;
    private String auctionId;
    private String bidderId;
    private Long amount;
    private Long placedAtEpochMs;

    protected BidEntity() {
    }

    public BidEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getPlacedAtEpochMs() {
        return placedAtEpochMs;
    }

    public void setPlacedAtEpochMs(Long placedAtEpochMs) {
        this.placedAtEpochMs = placedAtEpochMs;
    }
}
