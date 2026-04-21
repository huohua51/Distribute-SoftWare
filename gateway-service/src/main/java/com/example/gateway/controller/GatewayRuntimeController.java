package com.example.gateway.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class GatewayRuntimeController {

    @Value("${app.runtime.message:gateway-service local default}")
    private String runtimeMessage;

    @GetMapping("/gateway/runtime")
    public Map<String, Object> runtimeConfig() {
        return Map.of(
                "service", "gateway-service",
                "runtimeMessage", runtimeMessage
        );
    }
}
