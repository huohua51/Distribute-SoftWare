package com.example.seckill.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic seckillOrderTopic(@Value("${app.kafka.order-topic:seckill-order-topic}") String topicName) {
        return TopicBuilder.name(topicName).partitions(6).replicas(1).build();
    }
}
