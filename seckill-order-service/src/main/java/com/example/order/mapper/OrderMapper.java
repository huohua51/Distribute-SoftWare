package com.example.order.mapper;

import com.example.order.entity.OrderDO;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper {

    @Insert("""
            insert into orders(order_id, user_id, product_id, product_name, quantity, status, created_at, updated_at)
            values(#{orderId}, #{userId}, #{productId}, #{productName}, #{quantity}, #{status}, now(), now())
            """)
    int insert(OrderDO order);

    @Select("""
            select id, order_id, user_id, product_id, product_name, quantity, status, created_at, updated_at
            from orders
            where order_id = #{orderId}
            """)
    OrderDO findByOrderId(@Param("orderId") Long orderId);

    @Select("""
            select id, order_id, user_id, product_id, product_name, quantity, status, created_at, updated_at
            from orders
            where user_id = #{userId}
            order by id desc
            """)
    List<OrderDO> findByUserId(@Param("userId") Long userId);

    @Select("""
            select id, order_id, user_id, product_id, product_name, quantity, status, created_at, updated_at
            from orders
            where user_id = #{userId}
              and product_id = #{productId}
            limit 1
            """)
    OrderDO findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Update("""
            update orders
            set status = #{status},
                updated_at = now()
            where order_id = #{orderId}
            """)
    int updateStatus(@Param("orderId") Long orderId, @Param("status") String status);
}
