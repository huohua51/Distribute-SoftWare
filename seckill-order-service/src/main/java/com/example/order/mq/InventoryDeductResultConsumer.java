package com.example.order.mq;

import com.example.common.mq.InventoryDeductResultMessage;
import com.example.common.support.KafkaTopics;
import com.example.common.support.OrderStatus;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryDeductResultConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public InventoryDeductResultConsumer(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_RESULT_TOPIC, groupId = "order-service-group")
    public void consume(String payload) throws Exception {
        InventoryDeductResultMessage message = objectMapper.readValue(payload, InventoryDeductResultMessage.class);
        orderService.updateOrderStatus(
                message.orderId(),
                message.success() ? OrderStatus.CREATED.name() : OrderStatus.FAILED.name()
        );
    }
}
