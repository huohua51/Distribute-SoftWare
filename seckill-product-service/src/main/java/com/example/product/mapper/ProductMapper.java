package com.example.product.mapper;

import com.example.product.entity.ProductDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper {

    @Insert("""
            insert into product(product_id, product_name, status, created_at, updated_at)
            values(#{productId}, #{productName}, #{status}, now(), now())
            on duplicate key update
                product_name = values(product_name),
                status = values(status),
                updated_at = now()
            """)
    int upsert(ProductDO product);

    @Select("""
            select id, product_id, product_name, status, created_at, updated_at
            from product
            where product_id = #{productId}
            """)
    ProductDO findByProductId(@Param("productId") Long productId);
}
