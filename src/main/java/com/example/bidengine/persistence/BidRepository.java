package com.example.bidengine.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<BidEntity, String> {
    List<BidEntity> findTop50ByAuctionIdOrderByAmountDesc(String auctionId);
}
