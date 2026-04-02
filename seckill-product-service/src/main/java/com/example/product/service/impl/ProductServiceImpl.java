package com.example.product.service.impl;

import com.example.common.support.BusinessException;
import com.example.product.entity.ProductDO;
import com.example.product.mapper.ProductMapper;
import com.example.product.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public ProductDO createOrUpdate(Long productId, String productName) {
        ProductDO product = new ProductDO();
        product.setProductId(productId);
        product.setProductName(productName);
        product.setStatus("ONLINE");
        productMapper.upsert(product);
        return getByProductId(productId);
    }

    @Override
    public ProductDO getByProductId(Long productId) {
        ProductDO product = productMapper.findByProductId(productId);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return product;
    }
}
