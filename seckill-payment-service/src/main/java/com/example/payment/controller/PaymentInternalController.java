package com.example.payment.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.common.dto.PayOrderRequest;
import com.example.common.dto.PayOrderResponse;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payments")
public class PaymentInternalController {

    private final PaymentService paymentService;

    public PaymentInternalController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @SentinelResource("paymentPay")
    public PayOrderResponse pay(@RequestBody @Valid PayOrderRequest request) {
        return paymentService.pay(request);
    }
}
