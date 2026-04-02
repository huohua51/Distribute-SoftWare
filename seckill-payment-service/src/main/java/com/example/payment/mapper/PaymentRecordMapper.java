package com.example.payment.mapper;

import com.example.payment.entity.PaymentRecordDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentRecordMapper {

    @Insert("""
            insert into payment_record(payment_id, order_id, user_id, amount_fen, payment_status, fail_reason, created_at, updated_at)
            values(#{paymentId}, #{orderId}, #{userId}, #{amountFen}, #{paymentStatus}, #{failReason}, now(), now())
            """)
    int insert(PaymentRecordDO payment);

    @Select("""
            select id, payment_id, order_id, user_id, amount_fen, payment_status, fail_reason, created_at, updated_at
            from payment_record
            where order_id = #{orderId}
            limit 1
            """)
    PaymentRecordDO findByOrderId(@Param("orderId") Long orderId);

    @Update("""
            update payment_record
            set payment_status = #{paymentStatus},
                fail_reason = #{failReason},
                updated_at = now()
            where order_id = #{orderId}
            """)
    int updateStatus(@Param("orderId") Long orderId,
                     @Param("paymentStatus") String paymentStatus,
                     @Param("failReason") String failReason);
}
