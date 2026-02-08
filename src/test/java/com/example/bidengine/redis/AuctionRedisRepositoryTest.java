package com.example.bidengine.redis;

import com.example.bidengine.api.AuctionResponse;
import com.example.bidengine.api.BidResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

class AuctionRedisRepositoryTest {

    private RedisTemplate<String, String> redisTemplate;
    private AuctionRedisRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        repository = new AuctionRedisRepository(redisTemplate);
    }

    @Test
    void placeBidMapsErrors() {
        AuctionRedisRepository localRepository = new AuctionRedisRepository(
                new StubRedisTemplate(List.of("NOT_FOUND"))
        );
        var result = localRepository.placeBid("a1", "b1", "u1", 100, 10);

        assertThat(result.ok()).isFalse();
        assertThat(result.errorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void placeBidMapsSuccess() {
        AuctionRedisRepository localRepository = new AuctionRedisRepository(
                new StubRedisTemplate(List.of("OK", "150", "u1"))
        );
        var result = localRepository.placeBid("a1", "b1", "u1", 150, 10);

        assertThat(result.ok()).isTrue();
        assertThat(result.highestBid()).isEqualTo(150L);
        assertThat(result.highestBidderId()).isEqualTo("u1");
    }

    @Test
    void closeAuctionMapsSuccess() {
        AuctionRedisRepository localRepository = new AuctionRedisRepository(
                new StubRedisTemplate(List.of("CLOSED", "200", "u9"))
        );
        var result = localRepository.closeAuction("a1", 10);

        assertThat(result.ok()).isTrue();
        assertThat(result.status()).isEqualTo("CLOSED");
        assertThat(result.highestBid()).isEqualTo(200L);
        assertThat(result.highestBidderId()).isEqualTo("u9");
    }

    @Test
    void listTopBidsEmptyWhenNoRange() {
        ZSetOperations<String, String> zset = Mockito.mock(ZSetOperations.class);
        Mockito.when(redisTemplate.opsForZSet()).thenReturn(zset);
        Mockito.when(zset.reverseRange(any(), any(Long.class), any(Long.class))).thenReturn(null);

        List<BidResponse> bids = repository.listTopBids("a1", 5);

        assertThat(bids).isEmpty();
    }

    @Test
    void createAuctionStoresHash() {
        HashOperations<String, Object, Object> hashOps = Mockito.mock(HashOperations.class);
        Mockito.when(redisTemplate.opsForHash()).thenReturn(hashOps);

        AuctionResponse auction = new AuctionResponse(
                "a1", "s1", "title", "desc", "OPEN",
                100, 200L, 10, 20, 150L, "u1", 10, 10
        );
        repository.createAuction(auction);

        Mockito.verify(hashOps).putAll(Mockito.eq("auction:a1"), any(Map.class));
    }

    @Test
    void listTopBidsMapsResponses() {
        ZSetOperations<String, String> zset = Mockito.mock(ZSetOperations.class);
        HashOperations<String, Object, Object> hashOps = Mockito.mock(HashOperations.class);
        Mockito.when(redisTemplate.opsForZSet()).thenReturn(zset);
        Mockito.when(redisTemplate.opsForHash()).thenReturn(hashOps);

        Set<String> ids = Set.of("b1");
        Mockito.when(zset.reverseRange(any(), any(Long.class), any(Long.class))).thenReturn(ids);
        Mockito.when(hashOps.entries("bid:b1")).thenReturn(Map.of(
                "bidderId", "u1",
                "amount", "120",
                "placedAtEpochMs", "10"
        ));

        List<BidResponse> bids = repository.listTopBids("a1", 5);

        assertThat(bids).hasSize(1);
        assertThat(bids.get(0).bidderId()).isEqualTo("u1");
    }

    static class StubRedisTemplate extends RedisTemplate<String, String> {
        private final Object result;

        StubRedisTemplate(Object result) {
            this.result = result;
        }

        @Override
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            @SuppressWarnings("unchecked")
            T cast = (T) result;
            return cast;
        }

        @Override
        public <T> T execute(RedisScript<T> script, RedisSerializer<?> argsSerializer,
                             RedisSerializer<T> resultSerializer, List<String> keys, Object... args) {
            @SuppressWarnings("unchecked")
            T cast = (T) result;
            return cast;
        }
    }
}
