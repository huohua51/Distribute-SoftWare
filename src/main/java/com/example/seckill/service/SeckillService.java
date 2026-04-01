package com.example.seckill.service;

import com.example.seckill.dto.CreateProductRequest;
import com.example.seckill.dto.OrderQueryResponse;
import com.example.seckill.dto.StockResponse;
import java.util.List;

public interface SeckillService {

    Long submitSeckillOrder(Long userId, Long productId);

    OrderQueryResponse queryOrderByOrderId(Long orderId);

    List<OrderQueryResponse> queryOrdersByUserId(Long userId);

    StockResponse initProduct(CreateProductRequest request);

    StockResponse queryStock(Long productId);
}
