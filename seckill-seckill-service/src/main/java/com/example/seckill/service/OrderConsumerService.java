package com.example.seckill.service;

import com.example.seckill.mq.SeckillOrderMessage;

public interface OrderConsumerService {

    void createOrder(SeckillOrderMessage message);
}
