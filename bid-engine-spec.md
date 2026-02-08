## Bid Engine using DragonflyDB

Please build a bid engine like the one used by eBay using the dragonflydb which is an open source project at https://github.com/dragonflydb/dragonfly.

---

**Question**: What exact scope do you want: MVP auction service (create listing, place bids, close auction), or a full ad‑style bid engine?

**Answer**: MVP auction service

---

**Question**: Preferred language/runtime and framework?

**Answer**: Java spring boot

---

**Question**: Expected API shape (REST/GraphQL/gRPC) and any existing contracts?

**Answer**: REST

---

**Question**: Expected scale (QPS, concurrency), and any latency/consistency requirements?

Answer: 100000 QPS

---

**Question**: How should DragonflyDB be used: as primary store, cache, pub/sub, or all of the above?

**Answer**: as primary store and backed by PostgreSQL DB via Kakfa.
