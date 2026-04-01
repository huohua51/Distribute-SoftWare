package com.example.seckill.service.impl;

import com.example.seckill.dto.CreateProductRequest;
import com.example.seckill.dto.OrderQueryResponse;
import com.example.seckill.dto.StockResponse;
import com.example.seckill.entity.OrderDO;
import com.example.seckill.entity.ProductStockDO;
import com.example.seckill.mapper.OrderMapper;
import com.example.seckill.mapper.ProductStockMapper;
import com.example.seckill.mq.SeckillOrderMessage;
import com.example.seckill.service.SeckillService;
import com.example.seckill.support.BusinessException;
import com.example.seckill.support.OrderStatus;
import com.example.seckill.support.RedisKeys;
import com.example.seckill.support.SnowflakeIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeckillServiceImpl implements SeckillService {

    private static final DefaultRedisScript<Long> PRE_DEDUCT_SCRIPT = new DefaultRedisScript<>(
            """
                    local stockKey = KEYS[1]
                    local userKey = KEYS[2]
                    local orderId = ARGV[1]
                    local stock = tonumber(redis.call('get', stockKey) or '-1')
                    if redis.call('exists', userKey) == 1 then
                        return 2
                    end
                    if stock <= 0 then
                        return 0
                    end
                    redis.call('decr', stockKey)
                    redis.call('set', userKey, orderId)
                    return 1
                    """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final ProductStockMapper productStockMapper;
    private final OrderMapper orderMapper;
    private final String orderTopic;

    public SeckillServiceImpl(StringRedisTemplate redisTemplate,
                              KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              SnowflakeIdGenerator idGenerator,
                              ProductStockMapper productStockMapper,
                              OrderMapper orderMapper,
                              @Value("${app.kafka.order-topic:seckill-order-topic}") String orderTopic) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.productStockMapper = productStockMapper;
        this.orderMapper = orderMapper;
        this.orderTopic = orderTopic;
    }

    @Override
    public Long submitSeckillOrder(Long userId, Long productId) {
        ProductStockDO product = productStockMapper.findByProductId(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        OrderDO existingOrder = orderMapper.findByUserIdAndProductId(userId, productId);
        if (existingOrder != null) {
            throw new BusinessException("同一用户同一商品只能秒杀一次");
        }

        long orderId = idGenerator.nextId();
        String stockKey = RedisKeys.productStockKey(productId);
        String userOrderKey = RedisKeys.userOrderKey(userId, productId);
        Long result = redisTemplate.execute(PRE_DEDUCT_SCRIPT, List.of(stockKey, userOrderKey), String.valueOf(orderId));
        if (Objects.equals(result, 0L)) {
            throw new BusinessException("库存不足");
        }
        if (Objects.equals(result, 2L)) {
            throw new BusinessException("请勿重复下单");
        }
        if (!Objects.equals(result, 1L)) {
            throw new BusinessException("秒杀请求处理失败");
        }

        redisTemplate.opsForValue().set(
                RedisKeys.orderStatusKey(orderId),
                OrderStatus.PROCESSING.name(),
                Duration.ofHours(12)
        );

        SeckillOrderMessage message = new SeckillOrderMessage(
                String.valueOf(idGenerator.nextId()),
                orderId,
                userId,
                productId,
                product.getProductName(),
                1
        );

        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(orderTopic, String.valueOf(userId), payload);
        } catch (JsonProcessingException | DataAccessException ex) {
            rollbackReservation(userId, productId, orderId);
            throw new BusinessException("订单投递失败: " + ex.getMessage());
        }
        return orderId;
    }

    @Override
    public OrderQueryResponse queryOrderByOrderId(Long orderId) {
        OrderDO order = orderMapper.findByOrderId(orderId);
        if (order != null) {
            return toResponse(order);
        }

        String status = redisTemplate.opsForValue().get(RedisKeys.orderStatusKey(orderId));
        if (status == null) {
            throw new BusinessException("订单不存在");
        }
        return new OrderQueryResponse(orderId, null, null, null, null, status, null);
    }

    @Override
    public List<OrderQueryResponse> queryOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Override
    public StockResponse initProduct(CreateProductRequest request) {
        long productId = request.productId() == null ? idGenerator.nextId() : request.productId();
        ProductStockDO stock = new ProductStockDO();
        stock.setProductId(productId);
        stock.setProductName(request.productName());
        stock.setTotalStock(request.stock());
        stock.setAvailableStock(request.stock());
        stock.setVersion(0);
        productStockMapper.upsert(stock);
        redisTemplate.opsForValue().set(RedisKeys.productStockKey(productId), String.valueOf(request.stock()));
        return new StockResponse(productId, request.productName(), request.stock(), request.stock());
    }

    @Override
    public StockResponse queryStock(Long productId) {
        ProductStockDO stock = productStockMapper.findByProductId(productId);
        if (stock == null) {
            throw new BusinessException("商品不存在");
        }
        String redisStock = redisTemplate.opsForValue().get(RedisKeys.productStockKey(productId));
        Integer cachedStock = redisStock == null ? null : Integer.parseInt(redisStock);
        return new StockResponse(
                stock.getProductId(),
                stock.getProductName(),
                stock.getAvailableStock(),
                cachedStock
        );
    }

    private OrderQueryResponse toResponse(OrderDO order) {
        return new OrderQueryResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getProductId(),
                order.getProductName(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    private void rollbackReservation(Long userId, Long productId, Long orderId) {
        redisTemplate.opsForValue().increment(RedisKeys.productStockKey(productId));
        redisTemplate.delete(RedisKeys.userOrderKey(userId, productId));
        redisTemplate.delete(RedisKeys.orderStatusKey(orderId));
    }
}
