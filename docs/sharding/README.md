# ShardingSphere-Proxy 分库分表

本项目使用 `ShardingSphere-Proxy` 对订单表做分库分表，应用代码仍然面向逻辑表 `orders` 编写，不需要修改 `Mapper` SQL。

## 分片规则

- 分库键：`user_id`
- 分库算法：`ds_$->{user_id % 2}`
- 分表键：`order_id`
- 分表算法：`orders_$->{order_id % 2}`

实际节点如下：

- `ds_0.orders_0`
- `ds_0.orders_1`
- `ds_1.orders_0`
- `ds_1.orders_1`

其中：

- `product_stock` 作为单表，放在 `ds_0`
- `order_task` 作为单表，放在 `ds_0`

这样可以保持当前秒杀主链路不变，只把订单表交给代理层分片。

## 已提供文件

- `src/main/resources/application-sharding.yml`
  Spring Boot 在 `sharding` profile 下连接 ShardingSphere-Proxy。
- `docs/sharding/proxy-config.yaml`
  Proxy 的逻辑库与分片规则配置。
- `docs/sharding/server.yaml`
  Proxy 服务端口、用户与基础属性配置。
- `docs/sharding/schema-base.sql`
  创建 `order_ds_0`、`order_ds_1` 以及单表 `product_stock`、`order_task`。
- `docs/sharding/schema-order-ds-0.sql`
  在 `order_ds_0` 中创建 `orders_0`、`orders_1`。
- `docs/sharding/schema-order-ds-1.sql`
  在 `order_ds_1` 中创建 `orders_0`、`orders_1`。

## 启动步骤

### 1. 初始化 MySQL

如果使用项目自带的容器编排，`docker/mysql/init/01-init.sql` 会在 MySQL 首次启动时自动执行。

### 2. 配置并启动 ShardingSphere-Proxy

项目已经提供可直接用于容器挂载的配置：

- `docker/shardingsphere-proxy/conf/global.yaml`
- `docker/shardingsphere-proxy/conf/database-seckill_proxy.yaml`

其中逻辑库名为 `seckill_proxy`。

如果使用 `docker compose`，Proxy 会自动启动并对外暴露 `3307` 端口。

一键启动命令：

```bash
docker compose up -d --build
```

### 3. 使用分片 profile 启动应用

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=sharding
```

应用连接的是 Proxy：

- 地址：`jdbc:mysql://localhost:3307/seckill_proxy`

## 查询路由说明

### 按用户 ID 查询

`GET /api/seckill/orders?userId=1`

因为 `user_id` 是分库键，所以这类查询会优先命中单库，路由效率最好。

### 按订单 ID 查询

`GET /api/seckill/orders/{orderId}`

因为只携带了 `order_id`，表路由可以确定到 `orders_0` 或 `orders_1`，但数据库路由仍可能广播到两个库。这符合当前作业要求，但性能不如带 `userId` 的查询。

如果需要进一步优化，可以增加：

- `order_route` 路由表
- 在订单 ID 中编码库位信息
- 查询时同时传入 `userId`
