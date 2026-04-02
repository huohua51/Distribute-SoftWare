package com.example.seckill.mq;

import com.example.seckill.service.OrderConsumerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillOrderConsumer {

    private final ObjectMapper objectMapper;
    private final OrderConsumerService orderConsumerService;

    public SeckillOrderConsumer(ObjectMapper objectMapper, OrderConsumerService orderConsumerService) {
        this.objectMapper = objectMapper;
        this.orderConsumerService = orderConsumerService;
    }

    @KafkaListener(topics = "${app.kafka.order-topic:seckill-order-topic}", groupId = "${spring.kafka.consumer.group-id:seckill-order-group}")
    public void consume(String payload) throws JsonProcessingException {
        SeckillOrderMessage message = objectMapper.readValue(payload, SeckillOrderMessage.class);
        orderConsumerService.createOrder(message);
    }
}
