package com.example.order.mq;

import com.example.common.mq.PaymentResultMessage;
import com.example.common.support.KafkaTopics;
import com.example.common.support.OrderStatus;
import com.example.common.support.PaymentStatus;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public PaymentResultConsumer(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_RESULT_TOPIC, groupId = "order-service-group")
    public void consume(String payload) throws Exception {
        PaymentResultMessage message = objectMapper.readValue(payload, PaymentResultMessage.class);
        String orderStatus = PaymentStatus.SUCCESS.name().equals(message.paymentStatus())
                ? OrderStatus.PAID.name()
                : OrderStatus.PAYMENT_FAILED.name();
        orderService.updateOrderStatus(message.orderId(), orderStatus);
    }
}
