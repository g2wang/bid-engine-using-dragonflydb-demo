# Bid Engine using DragonflyDB (MVP)

This is an MVP auction service backed by DragonflyDB (Redis protocol) as the primary store, with PostgreSQL durability via Kafka.

## Architecture
- **Write path**: REST -> DragonflyDB (primary) -> Kafka events
- **Durability**: Kafka consumer persists auctions/bids into PostgreSQL
- **Atomic bid placement**: Lua script in DragonflyDB ensures correctness under high QPS

## Quick start

```bash
docker compose up -d
./gradlew bootRun
```

## REST API

### Create auction
```bash
curl -X POST http://localhost:8080/auctions \
  -H 'Content-Type: application/json' \
  -d '{
    "sellerId": "seller-1",
    "title": "Vintage Watch",
    "description": "1960s model",
    "startingPrice": 1000,
    "reservePrice": 1500,
    "startTimeEpochMs": 1738900000000,
    "endTimeEpochMs": 1738990000000
  }'
```

### Place bid
```bash
curl -X POST http://localhost:8080/auctions/{auctionId}/bids \
  -H 'Content-Type: application/json' \
  -d '{
    "bidderId": "user-9",
    "amount": 1200
  }'
```

### Get auction
```bash
curl http://localhost:8080/auctions/{auctionId}
```

### List top bids
```bash
curl http://localhost:8080/auctions/{auctionId}/bids?limit=50
```

### Close auction
```bash
curl -X POST http://localhost:8080/auctions/{auctionId}/close
```

## Notes on scale
- DragonflyDB handles atomicity and in-memory speed for ~100k QPS writes.
- Kafka provides durable event stream; Postgres is updated asynchronously.
- For horizontal scale, shard auctions by ID across DragonflyDB instances and Kafka partitions.
