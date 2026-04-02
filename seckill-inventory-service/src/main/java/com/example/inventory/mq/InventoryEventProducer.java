package com.example.inventory.mq;

import com.example.common.mq.InventoryDeductResultMessage;
import com.example.common.support.KafkaTopics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventoryEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendDeductResult(InventoryDeductResultMessage message) throws JsonProcessingException {
        kafkaTemplate.send(
                KafkaTopics.INVENTORY_RESULT_TOPIC,
                String.valueOf(message.orderId()),
                objectMapper.writeValueAsString(message)
        );
    }
}
