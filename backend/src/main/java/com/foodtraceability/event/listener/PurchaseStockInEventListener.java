package com.foodtraceability.event.listener;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
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
        
        Exception firstFailure = null;

        // DATA-007 防递归：扫码确认入库（TRACE_CONFIRM）时，本次入库的来源就是一张已存在的追溯码，
        // 若再走 generateMaterialTraceCodes 会「确认一张码 → 又生成一张新码 → 新码又待入库」，
        // 形成无休止的追溯码增殖。该来源只补成本入账。
        if (PurchaseStockInEvent.SOURCE_TRACE_CONFIRM.equals(event.getSourceType())) {
            log.info("扫码确认入库来源（TRACE_CONFIRM），跳过追溯码再生成，仅记录采购成本: stockinId={}",
                    event.getStockinId());
        } else {
            try {
                generateMaterialTraceCodes(event);
            } catch (Exception e) {
                log.error("采购入库联动-生成原料追溯码失败: stockinId={}, error={}",
                        event.getStockinId(), e.getMessage(), e);
                firstFailure = e;
            }
        }

        try {
            recordPurchaseCost(event);
        } catch (Exception e) {
            log.error("采购入库联动-记录采购成本失败: stockinId={}, error={}",
                    event.getStockinId(), e.getMessage(), e);
            if (firstFailure == null) {
                firstFailure = e;
            }
        }

        if (firstFailure != null) {
            // 假成功修复：原实现两步各自吞异常，外层仍打印「处理完成」，
            // 导致「原料无追溯码 / 采购成本未入账」被伪装成成功。
            log.error("采购入库事件联动不完整（追溯码或采购成本可能缺失，需人工补录）: stockinId={}",
                    event.getStockinId());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "采购入库联动失败（stockinId=" + event.getStockinId() + "）：" + firstFailure.getMessage());
        }

        log.info("采购入库事件处理完成: stockinId={}", event.getStockinId());
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
            log.error("生成原料追溯码失败: stockinId={}, productId={}, error={}",
                    event.getStockinId(), event.getProductId(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "生成原料追溯码失败（stockinId=" + event.getStockinId() + "）：" + e.getMessage());
        }
    }
    
    /**
     * 记录采购成本
     */
    private void recordPurchaseCost(PurchaseStockInEvent event) {
        log.info("记录采购成本: stockinId={}, amount={}",
                event.getStockinId(), event.getTotalAmount());

        // DATA-007 收尾：金额未知时**不写 0**。
        // cost_records.amount 是 NOT NULL DEFAULT 0（已核 information_schema），无法用 NULL 表达「金额未知」，
        // 所以宁可**不写成本行**并留 WARN，也不制造「有行、金额 0」的错账（等于把漏账伪装成 0 元成本）。
        // 补数路径：补到单价后按 stockinId/入库单号手工补录 cost_records。
        if (event.getTotalAmount() == null) {
            log.warn("采购入库成本待补：无单价，跳过成本行写入（cost_records.amount 为 NOT NULL，禁止落 0）: "
                            + "stockinId={}, stockinNo={}, productId={}, productName={}, quantity={}",
                    event.getStockinId(), event.getStockinNo(), event.getProductId(),
                    event.getProductName(), event.getQuantity());
            return;
        }

        try {
            CostRecordCreateDTO dto = new CostRecordCreateDTO();
            dto.setCostType(7); // 7-其他成本（采购入库）
            dto.setCostCenterId(event.getStoreId());
            // 金额：元（BigDecimal）转分（Long）；此处 amount 必非空（上面已拦截）
            BigDecimal amountYuan = event.getTotalAmount();
            dto.setAmount(amountYuan.multiply(BigDecimal.valueOf(100L)).longValue());
            dto.setPeriod(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
            dto.setRemark("采购入库成本 - " + event.getProductName()
                    + (event.getStockinNo() != null ? "（入库单:" + event.getStockinNo() + "）" : ""));
            dto.setCalculationMethod(1); // 1-实际发生

            costRecordService.create(dto);

            log.info("采购成本记录成功: stockinId={}", event.getStockinId());
        } catch (Exception e) {
            log.error("记录采购成本失败: stockinId={}, amount={}, error={}",
                    event.getStockinId(), event.getTotalAmount(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "记录采购成本失败（stockinId=" + event.getStockinId() + "）：" + e.getMessage());
        }
    }
}
