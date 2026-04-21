package com.example.seckill.client;

import com.example.common.dto.InventoryInitRequest;
import com.example.common.dto.InventoryReleaseRequest;
import com.example.common.dto.InventoryReserveRequest;
import com.example.common.dto.StockResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryRemoteClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public InventoryRemoteClient(RestTemplate restTemplate,
                                 @Value("${app.services.inventory-url:http://seckill-inventory-service}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void reserve(Long userId, Long productId, Long orderId) {
        restTemplate.postForLocation(
                baseUrl + "/internal/inventory/reservations",
                new InventoryReserveRequest(userId, productId, orderId)
        );
    }

    public void release(Long userId, Long productId, Long orderId) {
        restTemplate.postForLocation(
                baseUrl + "/internal/inventory/reservations/release",
                new InventoryReleaseRequest(userId, productId, orderId)
        );
    }

    public StockResponse init(Long productId, Integer stock) {
        return restTemplate.postForObject(
                baseUrl + "/internal/inventory/products/init",
                new InventoryInitRequest(productId, stock),
                StockResponse.class
        );
    }

    public StockResponse query(Long productId) {
        return restTemplate.getForObject(baseUrl + "/internal/inventory/products/" + productId, StockResponse.class);
    }
}
