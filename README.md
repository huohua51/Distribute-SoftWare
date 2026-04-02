# 高并发秒杀下单系统

这是一个从 0 搭建的 `Spring Boot + Redis + Kafka + MySQL` 秒杀下单示例，满足课程作业里提到的核心要求：

- Redis 缓存库存，先在缓存层预扣减
- Kafka 异步创建订单，削峰填谷
- 雪花算法生成订单 ID
- 支持按 `userId` 和 `orderId` 查询订单
- 幂等控制：同一用户同一商品只允许秒杀一次
- 数据一致性：缓存预扣减 + 数据库最终扣减 + 失败补偿，避免超卖
- 选做：已提供 `ShardingSphere-Proxy` 分库分表实现

## 技术栈

- `Spring Boot 3`
- `Redis`
- `Kafka`
- `MySQL`
- `MyBatis`
- `Lombok`

## 业务流程

1. 管理员先初始化商品库存，写入 MySQL 和 Redis。
2. 用户发起秒杀请求。
3. Redis Lua 脚本原子执行：
   - 校验用户是否已抢购
   - 校验库存是否大于 0
   - 预扣减 Redis 库存
   - 写入用户抢购标记
4. 服务端生成雪花订单 ID，将下单消息投递到 Kafka。
5. Kafka 消费者异步处理：
   - 记录消息任务表，避免重复消费
   - 扣减 MySQL 库存
   - 创建订单
   - 回写订单状态
6. 如果异步落单失败，则执行缓存补偿，恢复 Redis 库存与用户抢购标记。

## 防重复与一致性设计

### 幂等性

- Redis 使用 `userId + productId` 标记，拦截重复秒杀请求
- MySQL `orders` 表建立 `(user_id, product_id)` 唯一索引
- Kafka 消费端使用 `order_task.message_id` 唯一约束，防止重复消费

### 防超卖

- Redis 预扣减库存，拦住大部分并发流量
- MySQL 更新库存时使用 `available_stock >= quantity` 条件扣减
- 当数据库扣减失败时，对 Redis 做库存回补和用户标记回滚

## 项目结构

```text
homework4
├─ docs
├─ docker
├─ load
├─ nginx
├─ scripts
├─ seckill-common
├─ seckill-user-service
├─ seckill-product-service
├─ seckill-inventory-service
├─ seckill-order-service
└─ seckill-seckill-service
```

当前已完成的核心代码位于：

```text
seckill-seckill-service/src/main/java/com/example/seckill
├── config
├── controller
├── dto
├── entity
├── mapper
├── mq
├── service
└── support
```

## 运行前准备

确保本地已启动：

- `MySQL 8+`
- `Redis`
- `Kafka`

默认配置在 `seckill-seckill-service/src/main/resources/application.yml`：

- MySQL: `localhost:3306/seckill_demo`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`


## 建库建表

先创建数据库：

```sql
create database seckill_demo default character set utf8mb4;
```

再执行：

```text
seckill-seckill-service/src/main/resources/schema.sql
```

## 启动项目

```bash
mvn -pl seckill-seckill-service spring-boot:run
```

## Docker 一键启动中间件

项目已提供 `docker-compose.yml`，可一键拉起：

- `MySQL`
- `Redis`
- `Kafka`
- `ShardingSphere-Proxy`

启动命令：

```bash
docker compose up -d --build
```

启动后端口如下：

- MySQL: `3306`
- Redis: `6379`
- Kafka: `9092`
- ShardingSphere-Proxy: `3307`

如果你要走分库分表版本，直接使用：

```bash
mvn -pl seckill-seckill-service spring-boot:run -Dspring-boot.run.profiles=sharding
```

此时应用会连接：

- `jdbc:mysql://localhost:3307/seckill_proxy`

## 接口示例

### 1. 初始化商品

`POST /api/seckill/products`

```json
{
  "productId": 1001,
  "productName": "iPhone 16",
  "stock": 20
}
```

### 2. 查询库存

`GET /api/seckill/products/1001/stock`

### 3. 提交秒杀请求

`POST /api/seckill/orders`

```json
{
  "userId": 1,
  "productId": 1001
}
```

返回示例：

```json
{
  "success": true,
  "message": "秒杀请求已受理",
  "data": {
    "orderId": 1912345678901234567
  }
}
```

### 4. 按订单 ID 查询

`GET /api/seckill/orders/{orderId}`

如果订单已异步创建完成，会返回订单详情；若仍在处理中，会返回状态 `PROCESSING`。

### 5. 按用户 ID 查询

`GET /api/seckill/orders?userId=1`

## ShardingSphere 分库分表

本项目已经提供 `ShardingSphere-Proxy` 方案，实现：

- `orders` 按 `user_id` 分库
- `orders` 按 `order_id` 分表

使用方式：

1. 执行分片初始化脚本：
   - `docs/sharding/schema-base.sql`
   - `docs/sharding/schema-order-ds-0.sql`
   - `docs/sharding/schema-order-ds-1.sql`
2. 使用 `docs/sharding/proxy-config.yaml` 启动 `ShardingSphere-Proxy`
3. 通过分片 profile 启动应用：

```bash
mvn -pl seckill-seckill-service spring-boot:run -Dspring-boot.run.profiles=sharding
```

详细说明见：

- `docs/sharding/README.md`
