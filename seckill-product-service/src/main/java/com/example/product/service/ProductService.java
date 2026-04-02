package com.example.product.service;

import com.example.product.entity.ProductDO;

public interface ProductService {

    ProductDO createOrUpdate(Long productId, String productName);

    ProductDO getByProductId(Long productId);
}
