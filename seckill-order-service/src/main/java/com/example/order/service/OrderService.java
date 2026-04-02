package com.example.order.service;

import com.example.common.dto.OrderQueryResponse;
import com.example.order.entity.OrderDO;
import com.example.order.entity.OrderTaskDO;
import java.util.List;

public interface OrderService {

    OrderDO findByOrderId(Long orderId);

    List<OrderQueryResponse> findByUserId(Long userId);

    OrderDO findByUserIdAndProductId(Long userId, Long productId);

    OrderTaskDO findTaskByMessageId(String messageId);

    void createProcessingTask(String messageId, Long orderId, Long userId, Long productId);

    void updateTaskStatus(String messageId, String taskStatus, String failReason);

    void createOrder(OrderDO order);
}
