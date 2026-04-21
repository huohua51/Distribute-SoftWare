package com.example.seckill.config;

import com.example.common.support.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderCreateTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATE_TOPIC).partitions(3).replicas(1).build();
    }
}
