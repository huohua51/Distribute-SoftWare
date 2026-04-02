package com.example.order.mq;

import com.example.common.mq.InventoryDeductMessage;
import com.example.common.support.KafkaTopics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendInventoryDeductMessage(InventoryDeductMessage message) throws JsonProcessingException {
        kafkaTemplate.send(
                KafkaTopics.INVENTORY_DEDUCT_TOPIC,
                String.valueOf(message.orderId()),
                objectMapper.writeValueAsString(message)
        );
    }
}
