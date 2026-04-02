package com.example.seckill.service.impl;

import com.example.common.support.OrderStatus;
import com.example.common.support.RedisKeys;
import com.example.inventory.service.InventoryService;
import com.example.order.entity.OrderDO;
import com.example.order.entity.OrderTaskDO;
import com.example.order.service.OrderService;
import com.example.seckill.mq.SeckillOrderMessage;
import com.example.seckill.service.OrderConsumerService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderConsumerServiceImpl implements OrderConsumerService {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final StringRedisTemplate redisTemplate;

    public OrderConsumerServiceImpl(OrderService orderService,
                                    InventoryService inventoryService,
                                    StringRedisTemplate redisTemplate) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(SeckillOrderMessage message) {
        OrderTaskDO existTask = orderService.findTaskByMessageId(message.messageId());
        if (existTask != null && "SUCCESS".equals(existTask.getTaskStatus())) {
            return;
        }

        if (existTask == null) {
            try {
                orderService.createProcessingTask(
                        message.messageId(),
                        message.orderId(),
                        message.userId(),
                        message.productId()
                );
            } catch (DuplicateKeyException ignored) {
                existTask = orderService.findTaskByMessageId(message.messageId());
                if (existTask != null && "SUCCESS".equals(existTask.getTaskStatus())) {
                    return;
                }
            }
        }

        OrderDO existingOrder = orderService.findByUserIdAndProductId(message.userId(), message.productId());
        if (existingOrder != null) {
            markSuccess(message);
            return;
        }

        boolean deducted = inventoryService.deductDbStock(message.productId(), message.quantity());
        if (!deducted) {
            compensate(message, "数据库扣减库存失败");
            return;
        }

        OrderDO order = new OrderDO();
        order.setOrderId(message.orderId());
        order.setUserId(message.userId());
        order.setProductId(message.productId());
        order.setProductName(message.productName());
        order.setQuantity(message.quantity());
        order.setStatus(OrderStatus.CREATED.name());

        try {
            orderService.createOrder(order);
            markSuccess(message);
        } catch (DuplicateKeyException ex) {
            markSuccess(message);
        }
    }

    private void markSuccess(SeckillOrderMessage message) {
        orderService.updateTaskStatus(message.messageId(), "SUCCESS", null);
        redisTemplate.opsForValue().set(RedisKeys.orderStatusKey(message.orderId()), OrderStatus.CREATED.name());
    }

    private void compensate(SeckillOrderMessage message, String reason) {
        orderService.updateTaskStatus(message.messageId(), "FAILED", reason);
        inventoryService.releaseReservation(message.userId(), message.productId(), message.orderId());
        redisTemplate.opsForValue().set(RedisKeys.orderStatusKey(message.orderId()), OrderStatus.FAILED.name());
    }
}
