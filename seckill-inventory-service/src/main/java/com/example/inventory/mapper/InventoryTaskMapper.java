package com.example.inventory.mapper;

import com.example.inventory.entity.InventoryTaskDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryTaskMapper {

    @Select("""
            select id, message_id, order_id, user_id, product_id, task_status, fail_reason, created_at, updated_at
            from inventory_task
            where message_id = #{messageId}
            limit 1
            """)
    InventoryTaskDO findByMessageId(@Param("messageId") String messageId);

    @Insert("""
            insert into inventory_task(message_id, order_id, user_id, product_id, task_status, fail_reason, created_at, updated_at)
            values(#{messageId}, #{orderId}, #{userId}, #{productId}, #{taskStatus}, #{failReason}, now(), now())
            """)
    int insert(InventoryTaskDO task);

    @Update("""
            update inventory_task
            set task_status = #{taskStatus},
                fail_reason = #{failReason},
                updated_at = now()
            where message_id = #{messageId}
            """)
    int updateStatus(@Param("messageId") String messageId,
                     @Param("taskStatus") String taskStatus,
                     @Param("failReason") String failReason);
}
