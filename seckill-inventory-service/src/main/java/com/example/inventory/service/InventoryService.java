package com.example.inventory.service;

import com.example.inventory.entity.ProductStockDO;

public interface InventoryService {

    ProductStockDO initStock(Long productId, Integer stock);

    ProductStockDO getStock(Long productId);

    Long reserveStock(Long userId, Long productId, Long orderId);

    void releaseReservation(Long userId, Long productId, Long orderId);

    boolean deductDbStock(Long productId, Integer quantity);

    Integer getRedisStock(Long productId);
}
