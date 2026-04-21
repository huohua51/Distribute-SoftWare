# seckill-seckill-service

当前已实现的秒杀核心服务模块，包含：

- Redis 预扣减库存
- Kafka 异步下单
- 雪花算法订单 ID
- 幂等控制与失败补偿
- ShardingSphere-Proxy 分库分表接入
