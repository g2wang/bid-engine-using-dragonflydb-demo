package com.example.bidengine.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auctions")
public class AuctionEntity {
    @Id
    private String id;
    private String sellerId;
    private String title;
    private String description;
    private String status;
    private Long startingPrice;
    private Long reservePrice;
    private Long startTimeEpochMs;
    private Long endTimeEpochMs;
    private Long highestBid;
    private String highestBidderId;
    private Long createdAtEpochMs;
    private Long updatedAtEpochMs;

    protected AuctionEntity() {
    }

    public AuctionEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(Long startingPrice) {
        this.startingPrice = startingPrice;
    }

    public Long getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(Long reservePrice) {
        this.reservePrice = reservePrice;
    }

    public Long getStartTimeEpochMs() {
        return startTimeEpochMs;
    }

    public void setStartTimeEpochMs(Long startTimeEpochMs) {
        this.startTimeEpochMs = startTimeEpochMs;
    }

    public Long getEndTimeEpochMs() {
        return endTimeEpochMs;
    }

    public void setEndTimeEpochMs(Long endTimeEpochMs) {
        this.endTimeEpochMs = endTimeEpochMs;
    }

    public Long getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(Long highestBid) {
        this.highestBid = highestBid;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public Long getCreatedAtEpochMs() {
        return createdAtEpochMs;
    }

    public void setCreatedAtEpochMs(Long createdAtEpochMs) {
        this.createdAtEpochMs = createdAtEpochMs;
    }

    public Long getUpdatedAtEpochMs() {
        return updatedAtEpochMs;
    }

    public void setUpdatedAtEpochMs(Long updatedAtEpochMs) {
        this.updatedAtEpochMs = updatedAtEpochMs;
    }
}
