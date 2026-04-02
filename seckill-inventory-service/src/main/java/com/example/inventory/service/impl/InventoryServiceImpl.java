package com.example.inventory.service.impl;

import com.example.common.support.BusinessException;
import com.example.common.support.RedisKeys;
import com.example.inventory.entity.ProductStockDO;
import com.example.inventory.mapper.ProductStockMapper;
import com.example.inventory.service.InventoryService;
import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final DefaultRedisScript<Long> PRE_DEDUCT_SCRIPT = new DefaultRedisScript<>(
            """
                    local stockKey = KEYS[1]
                    local userKey = KEYS[2]
                    local orderId = ARGV[1]
                    local stock = tonumber(redis.call('get', stockKey) or '-1')
                    if redis.call('exists', userKey) == 1 then
                        return 2
                    end
                    if stock <= 0 then
                        return 0
                    end
                    redis.call('decr', stockKey)
                    redis.call('set', userKey, orderId)
                    return 1
                    """,
            Long.class
    );

    private final ProductStockMapper productStockMapper;
    private final StringRedisTemplate redisTemplate;

    public InventoryServiceImpl(ProductStockMapper productStockMapper, StringRedisTemplate redisTemplate) {
        this.productStockMapper = productStockMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ProductStockDO initStock(Long productId, Integer stock) {
        ProductStockDO entity = new ProductStockDO();
        entity.setProductId(productId);
        entity.setTotalStock(stock);
        entity.setAvailableStock(stock);
        entity.setVersion(0);
        productStockMapper.upsert(entity);
        redisTemplate.opsForValue().set(RedisKeys.productStockKey(productId), String.valueOf(stock));
        return getStock(productId);
    }

    @Override
    public ProductStockDO getStock(Long productId) {
        ProductStockDO stock = productStockMapper.findByProductId(productId);
        if (stock == null) {
            throw new BusinessException("库存不存在");
        }
        return stock;
    }

    @Override
    public Long reserveStock(Long userId, Long productId, Long orderId) {
        Long result = redisTemplate.execute(
                PRE_DEDUCT_SCRIPT,
                List.of(RedisKeys.productStockKey(productId), RedisKeys.userOrderKey(userId, productId)),
                String.valueOf(orderId)
        );
        if (Objects.equals(result, 0L)) {
            throw new BusinessException("库存不足");
        }
        if (Objects.equals(result, 2L)) {
            throw new BusinessException("请勿重复下单");
        }
        if (!Objects.equals(result, 1L)) {
            throw new BusinessException("秒杀请求处理失败");
        }
        return result;
    }

    @Override
    public void releaseReservation(Long userId, Long productId, Long orderId) {
        redisTemplate.opsForValue().increment(RedisKeys.productStockKey(productId));
        redisTemplate.delete(RedisKeys.userOrderKey(userId, productId));
        redisTemplate.delete(RedisKeys.orderStatusKey(orderId));
    }

    @Override
    public boolean deductDbStock(Long productId, Integer quantity) {
        return productStockMapper.decreaseStock(productId, quantity) > 0;
    }

    @Override
    public Integer getRedisStock(Long productId) {
        String cached = redisTemplate.opsForValue().get(RedisKeys.productStockKey(productId));
        return cached == null ? null : Integer.parseInt(cached);
    }
}
