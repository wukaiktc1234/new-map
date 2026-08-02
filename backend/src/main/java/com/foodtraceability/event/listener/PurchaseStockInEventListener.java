package com.foodtraceability.event.listener;

import com.foodtraceability.dto.finance.CostRecordCreateDTO;
import com.foodtraceability.event.PurchaseStockInEvent;
import com.foodtraceability.service.MaterialTraceCodeService;
import com.foodtraceability.service.finance.CostRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 采购入库事件监听器
 * 处理采购入库后的数据联动
 */
@Component
public class PurchaseStockInEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(PurchaseStockInEventListener.class);
    

    public PurchaseStockInEventListener(MaterialTraceCodeService materialTraceCodeService, CostRecordService costRecordService) {
        this.materialTraceCodeService = materialTraceCodeService;
        this.costRecordService = costRecordService;
    }

    private final MaterialTraceCodeService materialTraceCodeService;
    
    private final CostRecordService costRecordService;
    
    /**
     * 监听采购入库完成事件
     * 1. 生成原料追溯码
     * 2. 生成财务应付记录
     *
     * <p>DF-002 修复：将 @EventListener 改为 @TransactionalEventListener(AFTER_COMMIT)，
     * 确保主事务（采购入库、库存增加、应付账款创建）提交后才触发监听器，
     * 避免主事务回滚后异步线程仍执行，导致下游（溯源码生成、成本记录）产生脏数据。
     * 与 OrderCompletedEventListener 保持一致的事件绑定模式。</p>
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePurchaseStockInEvent(PurchaseStockInEvent event) {
        log.info("处理采购入库完成事件: stockinId={}", event.getStockinId());
        
        try {
            generateMaterialTraceCodes(event);
            
            recordPurchaseCost(event);
            
            log.info("采购入库事件处理完成: stockinId={}", event.getStockinId());
        } catch (Exception e) {
            log.error("处理采购入库事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 生成原料追溯码
     */
    private void generateMaterialTraceCodes(PurchaseStockInEvent event) {
        log.info("生成原料追溯码: productId={}, productName={}", 
                event.getProductId(), event.getProductName());
        
        try {
            var dto = new com.foodtraceability.dto.MaterialTraceCodeGenerateDTO();
            dto.setMaterialId(event.getProductId() != null ? String.valueOf(event.getProductId()) : null);
            dto.setMaterialName(event.getProductName());
            dto.setProductCode(event.getProductCode());
            dto.setSupplierId(event.getSupplierId() != null ? String.valueOf(event.getSupplierId()) : null);
            dto.setSupplierName(event.getSupplierName());
            dto.setPurchaseStockinId(event.getStockinId() != null ? String.valueOf(event.getStockinId()) : null);
            dto.setPurchaseOrderNo(event.getOrderNo());
            dto.setBatchNumber(event.getBatchNo());
            
            if (event.getProductionDate() != null) {
                dto.setProductionDate(LocalDate.parse(event.getProductionDate()));
            }
            
            dto.setShelfLifeDays(event.getShelfLifeDays());
            dto.setInboundTime(event.getStockinTime() != null ? 
                    event.getStockinTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            dto.setQuantity(event.getQuantity());
            dto.setUnit(event.getUnit());
            dto.setUnitPrice(event.getUnitPrice());
            dto.setWarehouseId(event.getWarehouseId() != null ? String.valueOf(event.getWarehouseId()) : null);
            dto.setWarehouseName(event.getWarehouseName());
            dto.setStoreId(event.getStoreId() != null ? String.valueOf(event.getStoreId()) : null);
            dto.setStoreName(event.getStoreName());
            dto.setGenerateCount(1);
            
            materialTraceCodeService.generateBatch(dto);
            
            log.info("原料追溯码生成成功: stockinId={}", event.getStockinId());
        } catch (Exception e) {
            log.error("生成原料追溯码失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录采购成本
     */
    private void recordPurchaseCost(PurchaseStockInEvent event) {
        log.info("记录采购成本: stockinId={}, amount={}",
                event.getStockinId(), event.getTotalAmount());

        try {
            CostRecordCreateDTO dto = new CostRecordCreateDTO();
            dto.setCostType(7); // 7-其他成本（采购入库）
            dto.setCostCenterId(event.getStoreId());
            // 金额：元（BigDecimal）转分（Long）
            BigDecimal amountYuan = event.getTotalAmount() != null
                    ? event.getTotalAmount() : BigDecimal.ZERO;
            dto.setAmount(amountYuan.multiply(BigDecimal.valueOf(100L)).longValue());
            dto.setPeriod(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
            dto.setRemark("采购入库成本 - " + event.getProductName());
            dto.setCalculationMethod(1); // 1-实际发生

            costRecordService.create(dto);

            log.info("采购成本记录成功: stockinId={}", event.getStockinId());
        } catch (Exception e) {
            log.error("记录采购成本失败: {}", e.getMessage(), e);
        }
    }
}
