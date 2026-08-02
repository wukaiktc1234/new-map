package com.foodtraceability.event.listener;

import com.foodtraceability.dto.MaterialTraceCodeGenerateDTO;
import com.foodtraceability.event.ReceiptConfirmationCompletedEvent;
import com.foodtraceability.service.MaterialTraceCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 收货确认完成事件监听器（到货确认 / 无单直收共用）
 *
 * <p>SR-7：确认收货后为每一条明细生成原料追溯码，保证单店直收与连锁到货
 * 均产出可追溯的批次证据，切换经营模式后数据仍可追溯。</p>
 *
 * <p>使用 {@code @TransactionalEventListener(AFTER_COMMIT) + @Async}，
 * 主事务（收货确认、库存增加、应付创建）提交后才执行；单项失败仅记录日志，
 * 不影响收货确认结果。</p>
 */
@Component
public class ReceiptConfirmationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReceiptConfirmationEventListener.class);

    private final MaterialTraceCodeService materialTraceCodeService;

    public ReceiptConfirmationEventListener(MaterialTraceCodeService materialTraceCodeService) {
        this.materialTraceCodeService = materialTraceCodeService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReceiptConfirmationCompleted(ReceiptConfirmationCompletedEvent event) {
        log.info("处理收货确认完成事件: confirmationId={}, receiptSource={}",
                event.getConfirmationId(), event.getReceiptSource());
        if (event.getItems() == null || event.getItems().isEmpty()) {
            return;
        }
        for (ReceiptConfirmationCompletedEvent.Item item : event.getItems()) {
            try {
                generateMaterialTraceCode(event, item);
            } catch (Exception e) {
                log.error("生成原料追溯码失败: confirmationId={}, materialId={}, 错误={}",
                        event.getConfirmationId(), item.getMaterialId(), e.getMessage(), e);
            }
        }
    }

    private void generateMaterialTraceCode(ReceiptConfirmationCompletedEvent event,
                                           ReceiptConfirmationCompletedEvent.Item item) {
        if (item.getMaterialId() == null || item.getConfirmedQuantity() == null
                || item.getConfirmedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        MaterialTraceCodeGenerateDTO dto = new MaterialTraceCodeGenerateDTO();
        dto.setMaterialId(String.valueOf(item.getMaterialId()));
        dto.setMaterialName(item.getMaterialName());
        dto.setSupplierId(event.getSupplierId() != null ? String.valueOf(event.getSupplierId()) : null);
        dto.setSupplierName(event.getSupplierName());
        dto.setPurchaseStockinId(String.valueOf(event.getConfirmationId()));
        dto.setBatchNumber(item.getBatchNo());
        dto.setProductionDate(item.getProductionDate());
        dto.setShelfLifeDays(computeShelfLifeDays(item));
        dto.setInboundTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dto.setQuantity(item.getConfirmedQuantity());
        dto.setUnit(item.getUnit());
        dto.setStoreId(event.getStoreId());
        dto.setStoreName(event.getStoreName());
        dto.setEntryType("direct".equals(event.getReceiptSource()) ? "supplier" : "order");
        dto.setGenerateCount(1);

        materialTraceCodeService.generateBatch(dto);
        log.info("原料追溯码生成成功: confirmationId={}, materialId={}, quantity={}",
                event.getConfirmationId(), item.getMaterialId(), item.getConfirmedQuantity());
    }

    private Integer computeShelfLifeDays(ReceiptConfirmationCompletedEvent.Item item) {
        if (item.getProductionDate() != null && item.getExpiryDate() != null) {
            long days = ChronoUnit.DAYS.between(item.getProductionDate(), item.getExpiryDate());
            if (days > 0) {
                return (int) days;
            }
        }
        return null;
    }
}
