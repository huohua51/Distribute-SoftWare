package com.example.payment.service.impl;

import com.example.common.dto.PayOrderRequest;
import com.example.common.dto.PayOrderResponse;
import com.example.common.mq.PaymentResultMessage;
import com.example.common.support.PaymentStatus;
import com.example.payment.entity.PaymentRecordDO;
import com.example.payment.mapper.PaymentRecordMapper;
import com.example.payment.mq.PaymentEventProducer;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements com.example.payment.service.PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentServiceImpl(PaymentRecordMapper paymentRecordMapper, PaymentEventProducer paymentEventProducer) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayOrderResponse pay(PayOrderRequest request) {
        PaymentRecordDO existing = paymentRecordMapper.findByOrderId(request.orderId());
        if (existing != null) {
            return new PayOrderResponse(existing.getOrderId(), existing.getPaymentId(), existing.getPaymentStatus());
        }

        long paymentId = Math.abs(ThreadLocalRandom.current().nextLong());
        PaymentRecordDO payment = new PaymentRecordDO();
        payment.setPaymentId(paymentId);
        payment.setOrderId(request.orderId());
        payment.setUserId(request.userId());
        payment.setAmountFen(request.amountFen());
        payment.setPaymentStatus(PaymentStatus.SUCCESS.name());
        paymentRecordMapper.insert(payment);

        try {
            paymentEventProducer.sendPaymentResult(new PaymentResultMessage(
                    String.valueOf(paymentId),
                    paymentId,
                    request.orderId(),
                    request.userId(),
                    PaymentStatus.SUCCESS.name(),
                    null
            ));
        } catch (Exception ex) {
            paymentRecordMapper.updateStatus(request.orderId(), PaymentStatus.FAILED.name(), ex.getMessage());
            throw new IllegalStateException("支付结果消息发送失败", ex);
        }

        return new PayOrderResponse(request.orderId(), paymentId, PaymentStatus.SUCCESS.name());
    }
}
