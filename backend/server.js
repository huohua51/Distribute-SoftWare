const express = require("express");
const Redis = require("ioredis");
const mysql = require("mysql2/promise");
const { Client: ESClient } = require("@elastic/elasticsearch");

const app = express();
app.use(express.json());

const port = Number(process.env.PORT || 3000);
const instanceName = process.env.INSTANCE_NAME || `backend-${port}`;

const redis = new Redis(process.env.REDIS_URL || "redis://redis:6379", {
  maxRetriesPerRequest: 1,
  lazyConnect: false
});

const masterPool = mysql.createPool({
  host: process.env.MYSQL_MASTER_HOST || "mysql-master",
  user: process.env.MYSQL_USER || "root",
  password: process.env.MYSQL_PASSWORD || "rootpass",
  database: process.env.MYSQL_DATABASE || "shop",
  waitForConnections: true,
  connectionLimit: 10
});

const slavePool = mysql.createPool({
  host: process.env.MYSQL_SLAVE_HOST || "mysql-slave",
  user: process.env.MYSQL_USER || "root",
  password: process.env.MYSQL_PASSWORD || "rootpass",
  database: process.env.MYSQL_DATABASE || "shop",
  waitForConnections: true,
  connectionLimit: 10
});

const es = new ESClient({
  node: process.env.ES_HOST || "http://elasticsearch:9200"
});

const ES_INDEX = "products";
let totalRequests = 0;

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function randomCacheTtl() {
  return 60 + Math.floor(Math.random() * 30);
}

async function readFromSlave(sql, params) {
  try {
    const [rows] = await slavePool.query(sql, params);
    return { rows, source: "mysql-slave" };
  } catch {
    const [rows] = await masterPool.query(sql, params);
    return { rows, source: "mysql-master-fallback" };
  }
}

async function writeToMaster(sql, params) {
  const [result] = await masterPool.query(sql, params);
  return result;
}

async function syncProductsToES() {
  try {
    const { rows } = await readFromSlave("SELECT * FROM products", []);
    const exists = await es.indices.exists({ index: ES_INDEX });
    if (!exists) {
      await es.indices.create({
        index: ES_INDEX,
        body: {
          mappings: {
            properties: {
              name: { type: "text", analyzer: "standard" },
              description: { type: "text", analyzer: "standard" },
              price: { type: "float" },
              stock: { type: "integer" }
            }
          }
        }
      });
    }

    for (const product of rows) {
      await es.index({
        index: ES_INDEX,
        id: String(product.id),
        body: {
          name: product.name,
          description: product.description,
          price: Number(product.price),
          stock: product.stock
        }
      });
    }
    await es.indices.refresh({ index: ES_INDEX });
    console.log(`[${instanceName}] Synced ${rows.length} products to ES`);
  } catch (err) {
    console.error(`[${instanceName}] ES sync failed: ${err.message}`);
  }
}

async function getProductWithCache(id) {
  const cacheKey = `product:${id}`;
  const nullKey = `product:null:${id}`;
  const lockKey = `lock:product:${id}`;

  const cached = await redis.get(cacheKey);
  if (cached) {
    return { data: JSON.parse(cached), source: "redis-cache" };
  }

  const nullCached = await redis.get(nullKey);
  if (nullCached) {
    return { data: null, source: "null-placeholder" };
  }

  const lockAcquired = await redis.set(lockKey, instanceName, "EX", 10, "NX");

  if (lockAcquired) {
    try {
      const { rows, source: dbSource } = await readFromSlave(
        "SELECT * FROM products WHERE id = ?",
        [id]
      );
      const product = rows[0] || null;

      if (!product) {
        await redis.set(nullKey, "1", "EX", 30);
        return { data: null, source: "database-null" };
      }

      await redis.set(cacheKey, JSON.stringify(product), "EX", randomCacheTtl());
      return { data: product, source: `${dbSource}-rebuild-cache` };
    } finally {
      await redis.del(lockKey);
    }
  }

  for (let retry = 0; retry < 5; retry++) {
    await sleep(50);
    const retryCache = await redis.get(cacheKey);
    if (retryCache) {
      return { data: JSON.parse(retryCache), source: "redis-cache-after-wait" };
    }
  }

  const { rows } = await readFromSlave("SELECT * FROM products WHERE id = ?", [id]);
  return { data: rows[0] || null, source: "database-fallback" };
}

// ---------- Routes ----------

app.get("/api/health", async (_req, res) => {
  let redisOk = true;
  let masterOk = true;
  let slaveOk = true;
  let esOk = true;

  try { await redis.ping(); } catch { redisOk = false; }
  try { await masterPool.query("SELECT 1"); } catch { masterOk = false; }
  try { await slavePool.query("SELECT 1"); } catch { slaveOk = false; }
  try { await es.ping(); } catch { esOk = false; }

  res.json({
    ok: true,
    instance: instanceName,
    totalRequests,
    redis: redisOk ? "ok" : "down",
    mysqlMaster: masterOk ? "ok" : "down",
    mysqlSlave: slaveOk ? "ok" : "down",
    elasticsearch: esOk ? "ok" : "down"
  });
});

app.get("/api/products/:id", async (req, res) => {
  totalRequests++;
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
      `[${instanceName}] #${totalRequests} product=${id} source=${result.source}`
    );

    return res.json({
      instance: instanceName,
      source: result.source,
      totalRequests,
      product: result.data
    });
  } catch (err) {
    console.error(`[${instanceName}]`, err.message);
    return res.status(500).json({ message: "Internal server error", instance: instanceName });
  }
});

app.get("/api/products", async (_req, res) => {
  totalRequests++;
  try {
    const { rows, source } = await readFromSlave("SELECT * FROM products", []);
    res.json({ instance: instanceName, source, products: rows });
  } catch (err) {
    console.error(`[${instanceName}]`, err.message);
    res.status(500).json({ message: "Internal server error" });
  }
});

app.post("/api/products", async (req, res) => {
  totalRequests++;
  const { name, price, stock, description } = req.body;

  try {
    const result = await writeToMaster(
      "INSERT INTO products (name, price, stock, description) VALUES (?, ?, ?, ?)",
      [name, price, stock || 0, description || ""]
    );
    const newId = result.insertId;

    await redis.del(`product:${newId}`);

    try {
      await es.index({
        index: ES_INDEX,
        id: String(newId),
        body: { name, description: description || "", price, stock: stock || 0 }
      });
      await es.indices.refresh({ index: ES_INDEX });
    } catch {}

    console.log(`[${instanceName}] WRITE to master: new product id=${newId}`);
    res.status(201).json({
      instance: instanceName,
      source: "mysql-master-write",
      product: { id: newId, name, price, stock: stock || 0, description }
    });
  } catch (err) {
    console.error(`[${instanceName}]`, err.message);
    res.status(500).json({ message: "Internal server error" });
  }
});

app.put("/api/products/:id", async (req, res) => {
  totalRequests++;
  const { id } = req.params;
  const { name, price, stock, description } = req.body;

  try {
    await writeToMaster(
      "UPDATE products SET name=?, price=?, stock=?, description=? WHERE id=?",
      [name, price, stock, description, id]
    );

    await redis.del(`product:${id}`);

    try {
      await es.index({
        index: ES_INDEX,
        id: String(id),
        body: { name, description: description || "", price, stock }
      });
      await es.indices.refresh({ index: ES_INDEX });
    } catch {}

    console.log(`[${instanceName}] WRITE to master: update product id=${id}`);
    res.json({
      instance: instanceName,
      source: "mysql-master-write",
      product: { id: Number(id), name, price, stock, description }
    });
  } catch (err) {
    console.error(`[${instanceName}]`, err.message);
    res.status(500).json({ message: "Internal server error" });
  }
});

app.get("/api/search", async (req, res) => {
  totalRequests++;
  const keyword = req.query.q || "";

  if (!keyword.trim()) {
    return res.json({ instance: instanceName, source: "elasticsearch", results: [] });
  }

  try {
    const body = await es.search({
      index: ES_INDEX,
      body: {
        query: {
          multi_match: {
            query: keyword,
            fields: ["name", "description"]
          }
        }
      }
    });

    const results = body.hits.hits.map((hit) => ({
      id: Number(hit._id),
      score: hit._score,
      ...hit._source
    }));

    res.json({ instance: instanceName, source: "elasticsearch", keyword, results });
  } catch (err) {
    console.error(`[${instanceName}] ES search error:`, err.message);
    res.status(500).json({ message: "Search unavailable" });
  }
});

app.get("/api/rw-test", async (_req, res) => {
  totalRequests++;
  try {
    const writeResult = await writeToMaster(
      "INSERT INTO products (name, price, stock, description) VALUES (?, ?, ?, ?)",
      [`RW-Test-${Date.now()}`, 1, 1, "read-write separation test"]
    );
    const newId = writeResult.insertId;

    await sleep(500);

    const { rows: slaveRows, source: slaveSource } = await readFromSlave(
      "SELECT * FROM products WHERE id = ?",
      [newId]
    );

    const [masterRows] = await masterPool.query(
      "SELECT * FROM products WHERE id = ?",
      [newId]
    );

    await writeToMaster("DELETE FROM products WHERE id = ?", [newId]);

    res.json({
      instance: instanceName,
      test: "read-write-separation",
      writtenTo: "mysql-master",
      newId,
      masterRead: { found: masterRows.length > 0, source: "mysql-master" },
      slaveRead: { found: slaveRows.length > 0, source: slaveSource }
    });
  } catch (err) {
    console.error(`[${instanceName}]`, err.message);
    res.status(500).json({ message: err.message });
  }
});

async function waitForMySQL(pool, label, maxRetries = 30) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      await pool.query("SELECT 1");
      console.log(`[${instanceName}] ${label} is ready`);
      return;
    } catch {
      console.log(`[${instanceName}] Waiting for ${label}... (${i + 1}/${maxRetries})`);
      await sleep(2000);
    }
  }
  console.error(`[${instanceName}] ${label} not available after ${maxRetries} retries`);
}

async function waitForES(maxRetries = 30) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      await es.ping();
      console.log(`[${instanceName}] ElasticSearch is ready`);
      return;
    } catch {
      console.log(`[${instanceName}] Waiting for ES... (${i + 1}/${maxRetries})`);
      await sleep(2000);
    }
  }
  console.error(`[${instanceName}] ES not available after ${maxRetries} retries`);
}

app.listen(port, async () => {
  console.log(`${instanceName} listening on port ${port}`);
  await waitForMySQL(masterPool, "MySQL master");
  await waitForMySQL(slavePool, "MySQL slave");
  await waitForES();
  await syncProductsToES();
});
