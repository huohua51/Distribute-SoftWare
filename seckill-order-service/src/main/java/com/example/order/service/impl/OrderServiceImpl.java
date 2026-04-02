package com.example.order.service.impl;

import com.example.common.dto.OrderQueryResponse;
import com.example.order.entity.OrderDO;
import com.example.order.entity.OrderTaskDO;
import com.example.order.mapper.OrderMapper;
import com.example.order.mapper.OrderTaskMapper;
import com.example.order.service.OrderService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderTaskMapper orderTaskMapper;

    public OrderServiceImpl(OrderMapper orderMapper, OrderTaskMapper orderTaskMapper) {
        this.orderMapper = orderMapper;
        this.orderTaskMapper = orderTaskMapper;
    }

    @Override
    public OrderDO findByOrderId(Long orderId) {
        return orderMapper.findByOrderId(orderId);
    }

    @Override
    public List<OrderQueryResponse> findByUserId(Long userId) {
        return orderMapper.findByUserId(userId).stream()
                .map(order -> new OrderQueryResponse(
                        order.getOrderId(),
                        order.getUserId(),
                        order.getProductId(),
                        order.getProductName(),
                        order.getQuantity(),
                        order.getStatus(),
                        order.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public OrderDO findByUserIdAndProductId(Long userId, Long productId) {
        return orderMapper.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public OrderTaskDO findTaskByMessageId(String messageId) {
        return orderTaskMapper.findByMessageId(messageId);
    }

    @Override
    public void createProcessingTask(String messageId, Long orderId, Long userId, Long productId) {
        OrderTaskDO task = new OrderTaskDO();
        task.setMessageId(messageId);
        task.setOrderId(orderId);
        task.setUserId(userId);
        task.setProductId(productId);
        task.setTaskStatus("PROCESSING");
        orderTaskMapper.insert(task);
    }

    @Override
    public void updateTaskStatus(String messageId, String taskStatus, String failReason) {
        orderTaskMapper.updateStatus(messageId, taskStatus, failReason);
    }

    @Override
    public void createOrder(OrderDO order) {
        orderMapper.insert(order);
    }

    @Override
    public void updateOrderStatus(Long orderId, String status) {
        orderMapper.updateStatus(orderId, status);
    }
}
