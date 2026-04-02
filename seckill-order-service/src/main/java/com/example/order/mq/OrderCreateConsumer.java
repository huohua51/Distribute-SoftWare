package com.example.order.mq;

import com.example.common.mq.InventoryDeductMessage;
import com.example.common.mq.OrderCreateMessage;
import com.example.common.support.KafkaTopics;
import com.example.common.support.OrderStatus;
import com.example.order.entity.OrderDO;
import com.example.order.entity.OrderTaskDO;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreateConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final OrderEventProducer orderEventProducer;

    public OrderCreateConsumer(ObjectMapper objectMapper,
                               OrderService orderService,
                               OrderEventProducer orderEventProducer) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.orderEventProducer = orderEventProducer;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CREATE_TOPIC, groupId = "order-service-group")
    public void consume(String payload) throws Exception {
        OrderCreateMessage message = objectMapper.readValue(payload, OrderCreateMessage.class);

        OrderTaskDO task = orderService.findTaskByMessageId(message.messageId());
        if (task != null && "SUCCESS".equals(task.getTaskStatus())) {
            return;
        }

        if (task == null) {
            try {
                orderService.createProcessingTask(message.messageId(), message.orderId(), message.userId(), message.productId());
            } catch (DuplicateKeyException ignored) {
                task = orderService.findTaskByMessageId(message.messageId());
                if (task != null && "SUCCESS".equals(task.getTaskStatus())) {
                    return;
                }
            }
        }

        if (orderService.findByOrderId(message.orderId()) == null) {
            OrderDO order = new OrderDO();
            order.setOrderId(message.orderId());
            order.setUserId(message.userId());
            order.setProductId(message.productId());
            order.setProductName(message.productName());
            order.setQuantity(message.quantity());
            order.setStatus(OrderStatus.PROCESSING.name());
            try {
                orderService.createOrder(order);
            } catch (DuplicateKeyException ignored) {
                // Idempotent duplicate consumption.
            }
        }

        orderEventProducer.sendInventoryDeductMessage(new InventoryDeductMessage(
                message.messageId(),
                message.orderId(),
                message.userId(),
                message.productId(),
                message.quantity()
        ));
        orderService.updateTaskStatus(message.messageId(), "SUCCESS", null);
    }
}
