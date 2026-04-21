package com.example.inventory.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.common.dto.InventoryInitRequest;
import com.example.common.dto.InventoryReleaseRequest;
import com.example.common.dto.InventoryReserveRequest;
import com.example.common.dto.StockResponse;
import com.example.inventory.entity.ProductStockDO;
import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/inventory")
public class InventoryInternalController {

    private final InventoryService inventoryService;

    public InventoryInternalController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/products/init")
    @SentinelResource("inventoryInit")
    public StockResponse initStock(@RequestBody @Valid InventoryInitRequest request) {
        ProductStockDO stock = inventoryService.initStock(request.productId(), request.stock());
        return new StockResponse(stock.getProductId(), null, stock.getAvailableStock(), inventoryService.getRedisStock(stock.getProductId()));
    }

    @GetMapping("/products/{productId}")
    @SentinelResource("inventoryQuery")
    public StockResponse queryStock(@PathVariable Long productId) {
        ProductStockDO stock = inventoryService.getStock(productId);
        return new StockResponse(stock.getProductId(), null, stock.getAvailableStock(), inventoryService.getRedisStock(productId));
    }

    @PostMapping("/reservations")
    @SentinelResource("inventoryReserve")
    public void reserve(@RequestBody @Valid InventoryReserveRequest request) {
        inventoryService.reserveStock(request.userId(), request.productId(), request.orderId());
    }

    @PostMapping("/reservations/release")
    @SentinelResource("inventoryRelease")
    public void release(@RequestBody @Valid InventoryReleaseRequest request) {
        inventoryService.releaseReservation(request.userId(), request.productId(), request.orderId());
    }
}
