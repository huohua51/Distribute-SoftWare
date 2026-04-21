package com.example.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.common.dto.OrderExistsResponse;
import com.example.common.dto.OrderQueryResponse;
import com.example.order.entity.OrderDO;
import com.example.order.service.OrderService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/orders")
public class OrderInternalController {

    private final OrderService orderService;

    public OrderInternalController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    @SentinelResource("orderFindByOrderId")
    public OrderQueryResponse findByOrderId(@PathVariable Long orderId) {
        OrderDO order = orderService.findByOrderId(orderId);
        if (order == null) {
            return null;
        }
        return new OrderQueryResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getProductId(),
                order.getProductName(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    @GetMapping
    @SentinelResource("orderFindByUserId")
    public List<OrderQueryResponse> findByUserId(@RequestParam Long userId) {
        return orderService.findByUserId(userId);
    }

    @GetMapping("/exists")
    @SentinelResource("orderExists")
    public OrderExistsResponse exists(@RequestParam Long userId, @RequestParam Long productId) {
        OrderDO order = orderService.findByUserIdAndProductId(userId, productId);
        return new OrderExistsResponse(order != null, order == null ? null : order.getOrderId(), order == null ? null : order.getStatus());
    }
}
