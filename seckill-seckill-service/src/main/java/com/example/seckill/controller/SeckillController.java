package com.example.seckill.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.dto.CreateProductRequest;
import com.example.common.dto.OrderQueryResponse;
import com.example.common.dto.PayOrderResponse;
import com.example.common.dto.SeckillOrderRequest;
import com.example.common.dto.StockResponse;
import com.example.seckill.service.SeckillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/products")
    public ApiResponse<StockResponse> initProduct(@RequestBody @Valid CreateProductRequest request) {
        return ApiResponse.success("商品初始化成功", seckillService.initProduct(request));
    }

    @GetMapping("/products/{productId}/stock")
    public ApiResponse<StockResponse> queryStock(@PathVariable @NotNull Long productId) {
        return ApiResponse.success(seckillService.queryStock(productId));
    }

    @PostMapping("/orders")
    public ApiResponse<Map<String, Long>> seckill(@RequestBody @Valid SeckillOrderRequest request) {
        Long orderId = seckillService.submitSeckillOrder(request.userId(), request.productId());
        return ApiResponse.success("秒杀请求已受理", Map.of("orderId", orderId));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<OrderQueryResponse> queryByOrderId(@PathVariable @NotNull Long orderId) {
        return ApiResponse.success(seckillService.queryOrderByOrderId(orderId));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderQueryResponse>> queryByUserId(@RequestParam @NotNull Long userId) {
        return ApiResponse.success(seckillService.queryOrdersByUserId(userId));
    }

    @PostMapping("/orders/{orderId}/pay")
    public ApiResponse<PayOrderResponse> payOrder(@PathVariable Long orderId,
                                                  @RequestParam Long userId,
                                                  @RequestParam(defaultValue = "1") Long amountFen) {
        return ApiResponse.success("支付请求已受理", seckillService.payOrder(orderId, userId, amountFen));
    }
}
