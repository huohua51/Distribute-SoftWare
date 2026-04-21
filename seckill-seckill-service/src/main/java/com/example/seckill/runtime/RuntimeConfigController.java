package com.example.seckill.runtime;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
@RequestMapping("/api/seckill/config")
public class RuntimeConfigController {

    @Value("${app.runtime.message:seckill-service local default}")
    private String runtimeMessage;

    @Value("${app.runtime.payment-enabled:true}")
    private boolean paymentEnabled;

    @Value("${app.limit.seckill-qps:20}")
    private int seckillQps;

    @GetMapping("/runtime")
    public Map<String, Object> currentRuntimeConfig() {
        return Map.of(
                "service", "seckill-seckill-service",
                "runtimeMessage", runtimeMessage,
                "paymentEnabled", paymentEnabled,
                "seckillQps", seckillQps
        );
    }
}
