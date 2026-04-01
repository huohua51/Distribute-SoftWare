package com.example.seckill.service.impl;

import com.example.seckill.entity.OrderDO;
import com.example.seckill.entity.OrderTaskDO;
import com.example.seckill.mapper.OrderMapper;
import com.example.seckill.mapper.OrderTaskMapper;
import com.example.seckill.mapper.ProductStockMapper;
import com.example.seckill.mq.SeckillOrderMessage;
import com.example.seckill.service.OrderConsumerService;
import com.example.seckill.support.OrderStatus;
import com.example.seckill.support.RedisKeys;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderConsumerServiceImpl implements OrderConsumerService {

    private final OrderTaskMapper orderTaskMapper;
    private final OrderMapper orderMapper;
    private final ProductStockMapper productStockMapper;
    private final StringRedisTemplate redisTemplate;

    public OrderConsumerServiceImpl(OrderTaskMapper orderTaskMapper,
                                    OrderMapper orderMapper,
                                    ProductStockMapper productStockMapper,
                                    StringRedisTemplate redisTemplate) {
        this.orderTaskMapper = orderTaskMapper;
        this.orderMapper = orderMapper;
        this.productStockMapper = productStockMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(SeckillOrderMessage message) {
        OrderTaskDO existTask = orderTaskMapper.findByMessageId(message.messageId());
        if (existTask != null && "SUCCESS".equals(existTask.getTaskStatus())) {
            return;
        }

        if (existTask == null) {
            OrderTaskDO task = new OrderTaskDO();
            task.setMessageId(message.messageId());
            task.setOrderId(message.orderId());
            task.setUserId(message.userId());
            task.setProductId(message.productId());
            task.setTaskStatus("PROCESSING");
            task.setFailReason(null);
            try {
                orderTaskMapper.insert(task);
            } catch (DuplicateKeyException ignored) {
                existTask = orderTaskMapper.findByMessageId(message.messageId());
                if (existTask != null && "SUCCESS".equals(existTask.getTaskStatus())) {
                    return;
                }
            }
        }

        OrderDO existingOrder = orderMapper.findByUserIdAndProductId(message.userId(), message.productId());
        if (existingOrder != null) {
            markSuccess(message);
            return;
        }

        int affectedRows = productStockMapper.decreaseStock(message.productId(), message.quantity());
        if (affectedRows <= 0) {
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
            orderMapper.insert(order);
            markSuccess(message);
        } catch (DuplicateKeyException ex) {
            markSuccess(message);
        }
    }

    private void markSuccess(SeckillOrderMessage message) {
        orderTaskMapper.updateStatus(message.messageId(), "SUCCESS", null);
        redisTemplate.opsForValue().set(RedisKeys.orderStatusKey(message.orderId()), OrderStatus.CREATED.name());
    }

    private void compensate(SeckillOrderMessage message, String reason) {
        orderTaskMapper.updateStatus(message.messageId(), "FAILED", reason);
        redisTemplate.opsForValue().increment(RedisKeys.productStockKey(message.productId()));
        redisTemplate.delete(RedisKeys.userOrderKey(message.userId(), message.productId()));
        redisTemplate.opsForValue().set(RedisKeys.orderStatusKey(message.orderId()), OrderStatus.FAILED.name());
    }
}
