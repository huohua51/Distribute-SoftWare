package com.example.seckill.client;

import com.example.common.dto.PayOrderRequest;
import com.example.common.dto.PayOrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentRemoteClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PaymentRemoteClient(RestTemplate restTemplate,
                               @Value("${app.services.payment-url:http://seckill-payment-service}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public PayOrderResponse pay(PayOrderRequest request) {
        return restTemplate.postForObject(baseUrl + "/internal/payments", request, PayOrderResponse.class);
    }
}
