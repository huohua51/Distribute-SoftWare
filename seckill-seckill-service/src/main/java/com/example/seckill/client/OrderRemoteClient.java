package com.example.seckill.client;

import com.example.common.dto.OrderExistsResponse;
import com.example.common.dto.OrderQueryResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderRemoteClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrderRemoteClient(RestTemplate restTemplate,
                             @Value("${app.services.order-url:http://seckill-order-service}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public OrderExistsResponse exists(Long userId, Long productId) {
        return restTemplate.getForObject(
                baseUrl + "/internal/orders/exists?userId=" + userId + "&productId=" + productId,
                OrderExistsResponse.class
        );
    }

    public OrderQueryResponse findByOrderId(Long orderId) {
        try {
            return restTemplate.getForObject(baseUrl + "/internal/orders/" + orderId, OrderQueryResponse.class);
        } catch (HttpClientErrorException ex) {
            return null;
        }
    }

    public List<OrderQueryResponse> findByUserId(Long userId) {
        ResponseEntity<OrderQueryResponse[]> response = restTemplate.getForEntity(
                baseUrl + "/internal/orders?userId=" + userId,
                OrderQueryResponse[].class
        );
        OrderQueryResponse[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }
}
