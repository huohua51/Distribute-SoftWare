package com.example.common.support;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String ORDER_CREATE_TOPIC = "order-create-topic";
    public static final String INVENTORY_DEDUCT_TOPIC = "inventory-deduct-topic";
    public static final String INVENTORY_RESULT_TOPIC = "inventory-deduct-result-topic";
    public static final String PAYMENT_RESULT_TOPIC = "payment-result-topic";
}
