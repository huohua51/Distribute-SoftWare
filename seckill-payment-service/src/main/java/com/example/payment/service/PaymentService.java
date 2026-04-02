package com.example.payment.service;

import com.example.common.dto.PayOrderRequest;
import com.example.common.dto.PayOrderResponse;

public interface PaymentService {

    PayOrderResponse pay(PayOrderRequest request);
}
