package com.example.bidengine.integration.redis;

import com.example.bidengine.api.AuctionResponse;
import com.example.bidengine.api.BidResponse;
import com.example.bidengine.redis.AuctionRedisRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.DockerClientFactory;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuctionRedisIntegrationTest {
    private static final GenericContainer<?> DRAGONFLY =
            new GenericContainer<>("dragonflydb/dragonfly:latest")
                    .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private AuctionRedisRepository repository;

    @BeforeAll
    static void startContainer() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available for Testcontainers");
        DRAGONFLY.start();
    }

    @AfterAll
    static void stopContainer() {
        DRAGONFLY.stop();
    }

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(DRAGONFLY.getHost(), DRAGONFLY.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        repository = new AuctionRedisRepository(template);
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void endToEndBidFlow() {
        long now = Instant.now().toEpochMilli();
        AuctionResponse auction = new AuctionResponse(
                "a1",
                "seller-1",
                "Vintage Watch",
                "1960s model",
                "OPEN",
                100,
                100L,
                now - 1000,
                now + 60_000,
                null,
                null,
                now,
                now
        );
        repository.createAuction(auction);

        var bidResult = repository.placeBid("a1", "b1", "u1", 150, now);
        assertThat(bidResult.ok()).isTrue();
        assertThat(bidResult.highestBid()).isEqualTo(150L);
        assertThat(bidResult.highestBidderId()).isEqualTo("u1");

        List<BidResponse> bids = repository.listTopBids("a1", 10);
        assertThat(bids).hasSize(1);
        assertThat(bids.get(0).amount()).isEqualTo(150L);

        var close = repository.closeAuction("a1", now + 1000);
        assertThat(close.ok()).isTrue();
        assertThat(close.highestBid()).isEqualTo(150L);
        assertThat(close.highestBidderId()).isEqualTo("u1");
    }
}
