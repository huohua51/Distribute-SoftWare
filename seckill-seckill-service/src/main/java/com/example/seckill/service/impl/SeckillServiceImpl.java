package com.example.seckill.service.impl;

import com.example.common.dto.CreateProductRequest;
import com.example.common.dto.OrderQueryResponse;
import com.example.common.dto.PayOrderRequest;
import com.example.common.dto.PayOrderResponse;
import com.example.common.dto.StockResponse;
import com.example.common.mq.OrderCreateMessage;
import com.example.common.support.BusinessException;
import com.example.common.support.KafkaTopics;
import com.example.common.support.OrderStatus;
import com.example.common.support.RedisKeys;
import com.example.product.entity.ProductDO;
import com.example.product.service.ProductService;
import com.example.seckill.client.InventoryRemoteClient;
import com.example.seckill.client.OrderRemoteClient;
import com.example.seckill.client.PaymentRemoteClient;
import com.example.seckill.service.SeckillService;
import com.example.seckill.support.SnowflakeIdGenerator;
import com.example.user.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeckillServiceImpl implements SeckillService {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final UserService userService;
    private final ProductService productService;
    private final InventoryRemoteClient inventoryRemoteClient;
    private final OrderRemoteClient orderRemoteClient;
    private final PaymentRemoteClient paymentRemoteClient;

    public SeckillServiceImpl(StringRedisTemplate redisTemplate,
                              KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              SnowflakeIdGenerator idGenerator,
                              UserService userService,
                              ProductService productService,
                              InventoryRemoteClient inventoryRemoteClient,
                              OrderRemoteClient orderRemoteClient,
                              PaymentRemoteClient paymentRemoteClient) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.userService = userService;
        this.productService = productService;
        this.inventoryRemoteClient = inventoryRemoteClient;
        this.orderRemoteClient = orderRemoteClient;
        this.paymentRemoteClient = paymentRemoteClient;
    }

    @Override
    public Long submitSeckillOrder(Long userId, Long productId) {
        userService.getOrCreateGuestUser(userId);
        ProductDO product = productService.getByProductId(productId);

        if (orderRemoteClient.exists(userId, productId).exists()) {
            throw new BusinessException("同一用户同一商品只能秒杀一次");
        }

        long orderId = idGenerator.nextId();
        inventoryRemoteClient.reserve(userId, productId, orderId);

        redisTemplate.opsForValue().set(
                RedisKeys.orderStatusKey(orderId),
                OrderStatus.PROCESSING.name(),
                Duration.ofHours(12)
        );

        OrderCreateMessage message = new OrderCreateMessage(
                String.valueOf(idGenerator.nextId()),
                orderId,
                userId,
                productId,
                product.getProductName(),
                1
        );

        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(KafkaTopics.ORDER_CREATE_TOPIC, String.valueOf(userId), payload);
        } catch (JsonProcessingException | DataAccessException ex) {
            inventoryRemoteClient.release(userId, productId, orderId);
            throw new BusinessException("订单投递失败: " + ex.getMessage());
        }
        return orderId;
    }

    @Override
    public OrderQueryResponse queryOrderByOrderId(Long orderId) {
        OrderQueryResponse order = orderRemoteClient.findByOrderId(orderId);
        if (order != null) {
            return order;
        }

        String status = redisTemplate.opsForValue().get(RedisKeys.orderStatusKey(orderId));
        if (status == null) {
            throw new BusinessException("订单不存在");
        }
        return new OrderQueryResponse(orderId, null, null, null, null, status, null);
    }

    @Override
    public List<OrderQueryResponse> queryOrdersByUserId(Long userId) {
        return orderRemoteClient.findByUserId(userId);
    }

    @Override
    public StockResponse initProduct(CreateProductRequest request) {
        long productId = request.productId() == null ? idGenerator.nextId() : request.productId();
        ProductDO product = productService.createOrUpdate(productId, request.productName());
        StockResponse stock = inventoryRemoteClient.init(productId, request.stock());
        return new StockResponse(
                product.getProductId(),
                product.getProductName(),
                stock.dbStock(),
                stock.redisStock()
        );
    }

    @Override
    public StockResponse queryStock(Long productId) {
        ProductDO product = productService.getByProductId(productId);
        StockResponse stock = inventoryRemoteClient.query(productId);
        return new StockResponse(
                product.getProductId(),
                product.getProductName(),
                stock.dbStock(),
                stock.redisStock()
        );
    }

    @Override
    public PayOrderResponse payOrder(Long orderId, Long userId, Long amountFen) {
        return paymentRemoteClient.pay(new PayOrderRequest(orderId, userId, amountFen));
    }
}
