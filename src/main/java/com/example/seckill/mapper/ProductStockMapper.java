package com.example.seckill.mapper;

import com.example.seckill.entity.ProductStockDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductStockMapper {

    @Insert("""
            insert into product_stock(product_id, product_name, total_stock, available_stock, version, created_at, updated_at)
            values(#{productId}, #{productName}, #{totalStock}, #{availableStock}, #{version}, now(), now())
            on duplicate key update
                product_name = values(product_name),
                total_stock = values(total_stock),
                available_stock = values(available_stock),
                updated_at = now()
            """)
    int upsert(ProductStockDO stock);

    @Select("""
            select id, product_id, product_name, total_stock, available_stock, version, created_at, updated_at
            from product_stock
            where product_id = #{productId}
            """)
    ProductStockDO findByProductId(@Param("productId") Long productId);

    @Update("""
            update product_stock
            set available_stock = available_stock - #{quantity},
                version = version + 1,
                updated_at = now()
            where product_id = #{productId}
              and available_stock >= #{quantity}
            """)
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
