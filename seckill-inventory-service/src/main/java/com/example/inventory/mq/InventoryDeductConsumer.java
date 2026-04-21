package com.example.inventory.mq;

import com.example.common.mq.InventoryDeductMessage;
import com.example.common.mq.InventoryDeductResultMessage;
import com.example.common.support.KafkaTopics;
import com.example.inventory.entity.InventoryTaskDO;
import com.example.inventory.mapper.InventoryTaskMapper;
import com.example.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventoryDeductConsumer {

    private final ObjectMapper objectMapper;
    private final InventoryTaskMapper inventoryTaskMapper;
    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;

    public InventoryDeductConsumer(ObjectMapper objectMapper,
                                   InventoryTaskMapper inventoryTaskMapper,
                                   InventoryService inventoryService,
                                   InventoryEventProducer inventoryEventProducer) {
        this.objectMapper = objectMapper;
        this.inventoryTaskMapper = inventoryTaskMapper;
        this.inventoryService = inventoryService;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_DEDUCT_TOPIC, groupId = "inventory-service-group")
    public void consume(String payload) throws Exception {
        InventoryDeductMessage message = objectMapper.readValue(payload, InventoryDeductMessage.class);
        InventoryTaskDO task = inventoryTaskMapper.findByMessageId(message.messageId());
        if (task != null && "SUCCESS".equals(task.getTaskStatus())) {
            return;
        }

        if (task == null) {
            InventoryTaskDO processing = new InventoryTaskDO();
            processing.setMessageId(message.messageId());
            processing.setOrderId(message.orderId());
            processing.setUserId(message.userId());
            processing.setProductId(message.productId());
            processing.setTaskStatus("PROCESSING");
            try {
                inventoryTaskMapper.insert(processing);
            } catch (DuplicateKeyException ignored) {
                task = inventoryTaskMapper.findByMessageId(message.messageId());
                if (task != null && "SUCCESS".equals(task.getTaskStatus())) {
                    return;
                }
            }
        }

        boolean success = inventoryService.deductDbStock(message.productId(), message.quantity());
        if (!success) {
            inventoryTaskMapper.updateStatus(message.messageId(), "FAILED", "库存数据库扣减失败");
            inventoryService.releaseReservation(message.userId(), message.productId(), message.orderId());
            inventoryEventProducer.sendDeductResult(new InventoryDeductResultMessage(
                    message.messageId(),
                    message.orderId(),
                    message.userId(),
                    message.productId(),
                    false,
                    "库存数据库扣减失败"
            ));
            return;
        }

        inventoryTaskMapper.updateStatus(message.messageId(), "SUCCESS", null);
        inventoryEventProducer.sendDeductResult(new InventoryDeductResultMessage(
                message.messageId(),
                message.orderId(),
                message.userId(),
                message.productId(),
                true,
                null
        ));
    }
}
