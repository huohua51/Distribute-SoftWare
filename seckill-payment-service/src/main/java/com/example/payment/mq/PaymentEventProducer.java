package com.example.payment.mq;

import com.example.common.mq.PaymentResultMessage;
import com.example.common.support.KafkaTopics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendPaymentResult(PaymentResultMessage message) throws JsonProcessingException {
        kafkaTemplate.send(
                KafkaTopics.PAYMENT_RESULT_TOPIC,
                String.valueOf(message.orderId()),
                objectMapper.writeValueAsString(message)
        );
    }
}
