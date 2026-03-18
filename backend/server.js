const express = require("express");
const Redis = require("ioredis");

const app = express();
const port = Number(process.env.PORT || 3000);
const instanceName = process.env.INSTANCE_NAME || `backend-${port}`;
const redisUrl = process.env.REDIS_URL || "redis://redis:6379";

const redis = new Redis(redisUrl, {
  maxRetriesPerRequest: 1,
  lazyConnect: false
});

const products = {
  "1": {
    id: 1,
    name: "High Concurrency Handbook",
    price: 99,
    stock: 120,
    description: "A demo product for cache-heavy read traffic."
  },
  "2": {
    id: 2,
    name: "Distributed Cache Notes",
    price: 79,
    stock: 80,
    description: "Used to demonstrate cache hit, miss and rebuild."
  },
  "3": {
    id: 3,
    name: "Nginx Practice Kit",
    price: 59,
    stock: 66,
    description: "A demo product for reverse proxy and static delivery."
  }
};

let totalRequests = 0;

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function randomCacheTtl() {
  return 60 + Math.floor(Math.random() * 30);
}

async function queryProductFromDb(id) {
  await sleep(120);
  return products[id] || null;
}

async function getProductWithCache(id) {
  const cacheKey = `product:${id}`;
  const nullKey = `product:null:${id}`;
  const lockKey = `lock:product:${id}`;

  const cached = await redis.get(cacheKey);
  if (cached) {
    return {
      data: JSON.parse(cached),
      source: "redis-cache"
    };
  }

  const nullCached = await redis.get(nullKey);
  if (nullCached) {
    return {
      data: null,
      source: "null-placeholder"
    };
  }

  const lockAcquired = await redis.set(lockKey, instanceName, "EX", 10, "NX");

  if (lockAcquired) {
    try {
      const product = await queryProductFromDb(id);

      if (!product) {
        await redis.set(nullKey, "1", "EX", 30);
        return {
          data: null,
          source: "database-null"
        };
      }

      await redis.set(cacheKey, JSON.stringify(product), "EX", randomCacheTtl());
      return {
        data: product,
        source: "database-rebuild-cache"
      };
    } finally {
      await redis.del(lockKey);
    }
  }

  for (let retry = 0; retry < 5; retry += 1) {
    await sleep(50);
    const retryCache = await redis.get(cacheKey);
    if (retryCache) {
      return {
        data: JSON.parse(retryCache),
        source: "redis-cache-after-wait"
      };
    }
  }

  const fallback = await queryProductFromDb(id);
  return {
    data: fallback,
    source: "database-fallback"
  };
}

app.get("/api/health", async (_req, res) => {
  let redisStatus = "connected";

  try {
    await redis.ping();
  } catch (error) {
    redisStatus = "unavailable";
  }

  res.json({
    ok: true,
    instance: instanceName,
    redisStatus,
    totalRequests
  });
});

app.get("/api/stats", (_req, res) => {
  res.json({
    instance: instanceName,
    totalRequests
  });
});

app.get("/api/products/:id", async (req, res) => {
  totalRequests += 1;
  const { id } = req.params;

  try {
    const result = await getProductWithCache(id);

    if (!result.data) {
      return res.status(404).json({
        message: "Product not found",
        instance: instanceName,
        source: result.source
      });
    }

    console.log(
      `[${instanceName}] request #${totalRequests} product=${id} source=${result.source}`
    );

    return res.json({
      instance: instanceName,
      source: result.source,
      totalRequests,
      product: result.data
    });
  } catch (error) {
    console.error(`[${instanceName}]`, error.message);
    return res.status(500).json({
      message: "Internal server error",
      instance: instanceName
    });
  }
});

app.get("/api/slow-query", async (_req, res) => {
  totalRequests += 1;
  await sleep(300);
  res.json({
    message: "slow endpoint",
    instance: instanceName,
    totalRequests
  });
});

app.listen(port, () => {
  console.log(`${instanceName} listening on port ${port}`);
});
