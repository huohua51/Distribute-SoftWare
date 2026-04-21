package com.example.seckill.service;

import com.example.common.dto.CreateProductRequest;
import com.example.common.dto.OrderQueryResponse;
import com.example.common.dto.PayOrderResponse;
import com.example.common.dto.StockResponse;
import java.util.List;

public interface SeckillService {

    Long submitSeckillOrder(Long userId, Long productId);

    OrderQueryResponse queryOrderByOrderId(Long orderId);

    List<OrderQueryResponse> queryOrdersByUserId(Long userId);

    StockResponse initProduct(CreateProductRequest request);

    StockResponse queryStock(Long productId);

    PayOrderResponse payOrder(Long orderId, Long userId, Long amountFen);
}
