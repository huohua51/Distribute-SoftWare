package com.example.seckill.service.impl;

import com.example.common.dto.CreateProductRequest;
import com.example.common.dto.OrderQueryResponse;
import com.example.common.dto.StockResponse;
import com.example.common.support.BusinessException;
import com.example.common.support.OrderStatus;
import com.example.common.support.RedisKeys;
import com.example.inventory.entity.ProductStockDO;
import com.example.inventory.service.InventoryService;
import com.example.order.entity.OrderDO;
import com.example.order.service.OrderService;
import com.example.product.entity.ProductDO;
import com.example.product.service.ProductService;
import com.example.seckill.mq.SeckillOrderMessage;
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
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final String orderTopic;

    public SeckillServiceImpl(StringRedisTemplate redisTemplate,
                              KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              SnowflakeIdGenerator idGenerator,
                              UserService userService,
                              ProductService productService,
                              InventoryService inventoryService,
                              OrderService orderService,
                              @Value("${app.kafka.order-topic:seckill-order-topic}") String orderTopic) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.userService = userService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.orderTopic = orderTopic;
    }

    @Override
    public Long submitSeckillOrder(Long userId, Long productId) {
        userService.getOrCreateGuestUser(userId);
        ProductDO product = productService.getByProductId(productId);

        OrderDO existingOrder = orderService.findByUserIdAndProductId(userId, productId);
        if (existingOrder != null) {
            throw new BusinessException("同一用户同一商品只能秒杀一次");
        }

        long orderId = idGenerator.nextId();
        inventoryService.reserveStock(userId, productId, orderId);

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
            inventoryService.releaseReservation(userId, productId, orderId);
            throw new BusinessException("订单投递失败: " + ex.getMessage());
        }
        return orderId;
    }

    @Override
    public OrderQueryResponse queryOrderByOrderId(Long orderId) {
        OrderDO order = orderService.findByOrderId(orderId);
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
        return orderService.findByUserId(userId);
    }

    @Override
    public StockResponse initProduct(CreateProductRequest request) {
        long productId = request.productId() == null ? idGenerator.nextId() : request.productId();
        ProductDO product = productService.createOrUpdate(productId, request.productName());
        ProductStockDO stock = inventoryService.initStock(productId, request.stock());
        return new StockResponse(
                product.getProductId(),
                product.getProductName(),
                stock.getAvailableStock(),
                inventoryService.getRedisStock(productId)
        );
    }

    @Override
    public StockResponse queryStock(Long productId) {
        ProductDO product = productService.getByProductId(productId);
        ProductStockDO stock = inventoryService.getStock(productId);
        return new StockResponse(
                product.getProductId(),
                product.getProductName(),
                stock.getAvailableStock(),
                inventoryService.getRedisStock(productId)
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
}
