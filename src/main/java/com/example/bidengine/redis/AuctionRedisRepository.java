package com.example.bidengine.redis;

import com.example.bidengine.api.BidResponse;
import com.example.bidengine.api.AuctionResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AuctionRedisRepository {
    private static final String AUCTION_KEY_PREFIX = "auction:";
    private static final String BIDS_KEY_SUFFIX = ":bids";
    private static final String BID_KEY_PREFIX = "bid:";

    private static final String PLACE_BID_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 0 then
              return { 'NOT_FOUND' }
            end
            local status = redis.call('HGET', KEYS[1], 'status')
            if status ~= 'OPEN' then
              return { 'NOT_OPEN' }
            end
            local startTime = tonumber(redis.call('HGET', KEYS[1], 'startTimeEpochMs'))
            local endTime = tonumber(redis.call('HGET', KEYS[1], 'endTimeEpochMs'))
            local now = tonumber(ARGV[1])
            if now < startTime then
              return { 'NOT_STARTED' }
            end
            if now > endTime then
              return { 'ENDED' }
            end
            local startingPrice = tonumber(redis.call('HGET', KEYS[1], 'startingPrice'))
            local highestBid = redis.call('HGET', KEYS[1], 'highestBid')
            local amount = tonumber(ARGV[4])
            if amount < startingPrice then
              return { 'BELOW_START' }
            end
            if highestBid and amount <= tonumber(highestBid) then
              return { 'BELOW_HIGHEST' }
            end
            redis.call('HSET', KEYS[3],
              'bidId', ARGV[2],
              'auctionId', ARGV[5],
              'bidderId', ARGV[3],
              'amount', ARGV[4],
              'placedAtEpochMs', ARGV[1]
            )
            redis.call('ZADD', KEYS[2], amount, ARGV[2])
            redis.call('HSET', KEYS[1],
              'highestBid', ARGV[4],
              'highestBidderId', ARGV[3],
              'updatedAtEpochMs', ARGV[1]
            )
            return { 'OK', ARGV[4], ARGV[3] }
            """;

    private static final String CLOSE_AUCTION_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 0 then
              return { 'NOT_FOUND' }
            end
            local status = redis.call('HGET', KEYS[1], 'status')
            if status ~= 'OPEN' then
              return { 'NOT_OPEN' }
            end
            redis.call('HSET', KEYS[1],
              'status', 'CLOSED',
              'updatedAtEpochMs', ARGV[1]
            )
            local highestBid = redis.call('HGET', KEYS[1], 'highestBid')
            local highestBidderId = redis.call('HGET', KEYS[1], 'highestBidderId')
            return { 'OK', highestBid, highestBidderId }
            """;

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List> placeBidScript;
    private final DefaultRedisScript<List> closeAuctionScript;

    public AuctionRedisRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.placeBidScript = new DefaultRedisScript<>(PLACE_BID_SCRIPT, List.class);
        this.closeAuctionScript = new DefaultRedisScript<>(CLOSE_AUCTION_SCRIPT, List.class);
    }

    public void createAuction(AuctionResponse auction) {
        String key = auctionKey(auction.auctionId());
        Map<String, String> fields = new HashMap<>();
        fields.put("auctionId", auction.auctionId());
        fields.put("sellerId", auction.sellerId());
        fields.put("title", auction.title());
        fields.put("description", auction.description() == null ? "" : auction.description());
        fields.put("status", auction.status());
        fields.put("startingPrice", String.valueOf(auction.startingPrice()));
        if (auction.reservePrice() != null) {
            fields.put("reservePrice", String.valueOf(auction.reservePrice()));
        }
        fields.put("startTimeEpochMs", String.valueOf(auction.startTimeEpochMs()));
        fields.put("endTimeEpochMs", String.valueOf(auction.endTimeEpochMs()));
        if (auction.highestBid() != null) {
            fields.put("highestBid", String.valueOf(auction.highestBid()));
        }
        if (auction.highestBidderId() != null) {
            fields.put("highestBidderId", auction.highestBidderId());
        }
        fields.put("createdAtEpochMs", String.valueOf(auction.createdAtEpochMs()));
        fields.put("updatedAtEpochMs", String.valueOf(auction.updatedAtEpochMs()));
        redisTemplate.opsForHash().putAll(key, fields);
    }

    public AuctionResponse getAuction(String auctionId) {
        String key = auctionKey(auctionId);
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return new AuctionResponse(
                auctionId,
                stringVal(map.get("sellerId")),
                stringVal(map.get("title")),
                stringVal(map.get("description")),
                stringVal(map.get("status")),
                longVal(map.get("startingPrice")),
                nullableLong(map.get("reservePrice")),
                longVal(map.get("startTimeEpochMs")),
                longVal(map.get("endTimeEpochMs")),
                nullableLong(map.get("highestBid")),
                stringVal(map.get("highestBidderId")),
                longVal(map.get("createdAtEpochMs")),
                longVal(map.get("updatedAtEpochMs"))
        );
    }

    public PlaceBidResult placeBid(String auctionId, String bidId, String bidderId, long amount, long nowEpochMs) {
        String auctionKey = auctionKey(auctionId);
        String bidsKey = bidsKey(auctionId);
        String bidKey = bidKey(bidId);
        List<String> keys = List.of(auctionKey, bidsKey, bidKey);
        Object[] args = new Object[]{
                String.valueOf(nowEpochMs),
                bidId,
                bidderId,
                String.valueOf(amount),
                auctionId
        };
        List<?> response = redisTemplate.execute(placeBidScript, keys, args);
        if (response == null || response.isEmpty()) {
            return PlaceBidResult.error("UNKNOWN");
        }
        String status = response.get(0).toString();
        if (!"OK".equals(status)) {
            return PlaceBidResult.error(status);
        }
        Long highestBid = response.size() > 1 && response.get(1) != null ? Long.parseLong(response.get(1).toString()) : null;
        String highestBidderId = response.size() > 2 && response.get(2) != null ? response.get(2).toString() : null;
        return PlaceBidResult.success(highestBid, highestBidderId);
    }

    public CloseAuctionResult closeAuction(String auctionId, long nowEpochMs) {
        String auctionKey = auctionKey(auctionId);
        List<String> keys = List.of(auctionKey);
        Object[] args = new Object[]{String.valueOf(nowEpochMs)};
        List<?> response = redisTemplate.execute(closeAuctionScript, keys, args);
        if (response == null || response.isEmpty()) {
            return CloseAuctionResult.error("UNKNOWN");
        }
        String status = response.get(0).toString();
        if (!"OK".equals(status)) {
            return CloseAuctionResult.error(status);
        }
        Long highestBid = response.size() > 1 && response.get(1) != null ? Long.parseLong(response.get(1).toString()) : null;
        String highestBidderId = response.size() > 2 && response.get(2) != null ? response.get(2).toString() : null;
        return CloseAuctionResult.success(highestBid, highestBidderId);
    }

    public List<BidResponse> listTopBids(String auctionId, int limit) {
        String bidsKey = bidsKey(auctionId);
        var range = redisTemplate.opsForZSet().reverseRange(bidsKey, 0, limit - 1);
        if (range == null || range.isEmpty()) {
            return List.of();
        }
        List<String> bidIds = new ArrayList<>(range);
        List<BidResponse> bids = new ArrayList<>();
        for (String bidId : bidIds) {
            Map<Object, Object> map = redisTemplate.opsForHash().entries(bidKey(bidId));
            if (map == null || map.isEmpty()) {
                continue;
            }
            bids.add(new BidResponse(
                    bidId,
                    auctionId,
                    stringVal(map.get("bidderId")),
                    longVal(map.get("amount")),
                    longVal(map.get("placedAtEpochMs"))
            ));
        }
        return bids;
    }

    private static String auctionKey(String auctionId) {
        return AUCTION_KEY_PREFIX + auctionId;
    }

    private static String bidsKey(String auctionId) {
        return AUCTION_KEY_PREFIX + auctionId + BIDS_KEY_SUFFIX;
    }

    private static String bidKey(String bidId) {
        return BID_KEY_PREFIX + bidId;
    }

    private static String stringVal(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private static long longVal(Object obj) {
        if (obj == null) {
            return 0L;
        }
        return Long.parseLong(obj.toString());
    }

    private static Long nullableLong(Object obj) {
        return obj == null ? null : Long.parseLong(obj.toString());
    }

    public record PlaceBidResult(boolean ok, String errorCode, Long highestBid, String highestBidderId) {
        public static PlaceBidResult success(Long highestBid, String highestBidderId) {
            return new PlaceBidResult(true, null, highestBid, highestBidderId);
        }

        public static PlaceBidResult error(String code) {
            return new PlaceBidResult(false, code, null, null);
        }
    }

    public record CloseAuctionResult(boolean ok, String errorCode, Long highestBid, String highestBidderId) {
        public static CloseAuctionResult success(Long highestBid, String highestBidderId) {
            return new CloseAuctionResult(true, null, highestBid, highestBidderId);
        }

        public static CloseAuctionResult error(String code) {
            return new CloseAuctionResult(false, code, null, null);
        }
    }
}
