package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.*;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseStockinScanServiceImpl implements PurchaseStockinScanService {
    
    private static final Logger log = LoggerFactory.getLogger(PurchaseStockinScanServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    

    public PurchaseStockinScanServiceImpl(PurchaseStockinMapper purchaseStockinMapper, MaterialTraceCodeMapper materialTraceCodeMapper, MaterialTraceCodeService materialTraceCodeService, MaterialLabelPrintService materialLabelPrintService, InventoryService inventoryService, FinanceVoucherService financeVoucherService, InventoryLogService inventoryLogService) {
        this.purchaseStockinMapper = purchaseStockinMapper;
        this.materialTraceCodeMapper = materialTraceCodeMapper;
        this.materialTraceCodeService = materialTraceCodeService;
        this.materialLabelPrintService = materialLabelPrintService;
        this.inventoryService = inventoryService;
        this.financeVoucherService = financeVoucherService;
        this.inventoryLogService = inventoryLogService;
    }

    private final PurchaseStockinMapper purchaseStockinMapper;
    
    private final MaterialTraceCodeMapper materialTraceCodeMapper;
    
    private final MaterialTraceCodeService materialTraceCodeService;
    
    private final MaterialLabelPrintService materialLabelPrintService;
    
    private final InventoryService inventoryService;
    
    private final FinanceVoucherService financeVoucherService;
    
    private final InventoryLogService inventoryLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScanConfirmResult scanConfirmStockin(PurchaseStockinScanDTO scanDTO) {
        log.info("扫描确认入库: stockinId={}, traceCode={}", scanDTO.getStockinId(), scanDTO.getTraceCode());
        
        ScanConfirmResult result = new ScanConfirmResult();
        result.setTraceCode(scanDTO.getTraceCode());
        
        MaterialTraceCode traceCode = materialTraceCodeService.getByTraceCode(scanDTO.getTraceCode());
        
        if (traceCode == null) {
            log.warn("追溯码不存在: {}", scanDTO.getTraceCode());
            return ScanConfirmResult.needIntervention("追溯码不存在", ManualInterventionDTO.TYPE_INVALID_CODE);
        }
        
        result.setMaterialName(traceCode.getMaterialName());
        result.setExpectedQuantity(traceCode.getQuantity());
        result.setScannedQuantity(scanDTO.getScannedQuantity());
        
        if (!isValidStatusForScan(traceCode.getStatus())) {
            log.warn("追溯码状态异常: traceCode={}, status={}", scanDTO.getTraceCode(), traceCode.getStatus());
            result.setSuccess(false);
            result.setMessage("追溯码状态异常: " + getStatusLabel(traceCode.getStatus()));
            result.setNeedManualIntervention(true);
            result.setAlertType(ManualInterventionDTO.TYPE_OTHER);
            return result;
        }
        
        if (isExpired(traceCode)) {
            log.warn("追溯码已过期: traceCode={}, expiryDate={}", scanDTO.getTraceCode(), traceCode.getExpiryDate());
            return ScanConfirmResult.needIntervention("原料已过期，到期日期: " + traceCode.getExpiryDate(), ManualInterventionDTO.TYPE_EXPIRED);
        }
        
        if (scanDTO.getScannedQuantity() != null) {
            BigDecimal expected = traceCode.getQuantity();
            BigDecimal actual = scanDTO.getScannedQuantity();
            
            if (actual.compareTo(expected) != 0) {
                log.warn("数量不符: expected={}, actual={}", expected, actual);
                result.setSuccess(false);
                result.setMessage("数量不符! 预期: " + expected + ", 实际: " + actual);
                result.setNeedManualIntervention(true);
                result.setAlertType(ManualInterventionDTO.TYPE_QUANTITY_MISMATCH);
                triggerQuantityAlert(traceCode, expected, actual, scanDTO.getStockinId());
                return result;
            }
        }
        
        traceCode.setStatus("active");
        traceCode.setScanTime(LocalDateTime.now());
        traceCode.setUpdateTime(LocalDateTime.now());
        materialTraceCodeMapper.updateById(traceCode);
        
        log.info("追溯码扫描确认成功: traceCode={}", scanDTO.getTraceCode());
        result.setSuccess(true);
        result.setMessage("扫描确认成功");
        
        checkAndAutoCompleteStockin(scanDTO.getStockinId());
        
        return result;
    }
    
    private boolean isValidStatusForScan(String status) {
        return "pending".equals(status) || "label_printed".equals(status);
    }
    
    private boolean isExpired(MaterialTraceCode traceCode) {
        if (traceCode.getExpiryDate() == null) {
            return false;
        }
        return traceCode.getExpiryDate().isBefore(java.time.LocalDate.now());
    }
    
    private String getStatusLabel(String status) {
        switch (status) {
            case "pending": return "待确认";
            case "label_printed": return "已打印标签";
            case "active": return "已入库";
            case "picked": return "已领用";
            case "used": return "已使用";
            case "returned": return "已退回";
            case "expired": return "已过期";
            default: return status;
        }
    }
    
    private void triggerQuantityAlert(MaterialTraceCode traceCode, BigDecimal expected, BigDecimal actual, Long stockinId) {
        log.warn("触发数量异常报警: stockinId={}, traceCode={}, expected={}, actual={}", 
            stockinId, traceCode.getTraceCode(), expected, actual);
    }
    
    private void checkAndAutoCompleteStockin(Long stockinId) {
        StockinProgressDTO progress = getStockinProgress(stockinId);
        if (progress != null && progress.isProgressComplete() && progress.isCanComplete()) {
            log.info("所有标签已扫描完成，自动完成入库: stockinId={}", stockinId);
            completeStockin(stockinId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ScanConfirmResult> batchScanConfirm(List<PurchaseStockinScanDTO> scanDTOs) {
        List<ScanConfirmResult> results = new ArrayList<>();
        for (PurchaseStockinScanDTO dto : scanDTOs) {
            results.add(scanConfirmStockin(dto));
        }
        return results;
    }

    @Override
    public StockinProgressDTO getStockinProgress(Long stockinId) {
        PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
        if (stockin == null) {
            return null;
        }
        
        List<MaterialTraceCode> codes = materialTraceCodeService.getByStockinId(stockinId);
        
        StockinProgressDTO progress = new StockinProgressDTO();
        progress.setStockinId(stockinId);
        // 使用getStockinCode()而非getStockinNo()（新实体类字段名）
        progress.setStockinNo(stockin.getStockinCode());
        progress.setStatus(stockin.getStatus() != null ? stockin.getStatus().toString() : "pending");
        
        int total = codes.size();
        int scanned = 0;
        int pending = 0;
        int errors = 0;
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal scannedQty = BigDecimal.ZERO;
        
        List<StockinProgressDTO.LabelProgressItem> labelItems = new ArrayList<>();
        
        for (MaterialTraceCode code : codes) {
            StockinProgressDTO.LabelProgressItem item = new StockinProgressDTO.LabelProgressItem();
            item.setTraceCodeId(code.getTraceCodeId());
            item.setTraceCode(code.getTraceCode());
            item.setMaterialName(code.getMaterialName());
            item.setQuantity(code.getQuantity());
            item.setStatus(code.getStatus());
            
            totalQty = totalQty.add(code.getQuantity() != null ? code.getQuantity() : BigDecimal.ZERO);
            
            if ("active".equals(code.getStatus())) {
                scanned++;
                scannedQty = scannedQty.add(code.getQuantity() != null ? code.getQuantity() : BigDecimal.ZERO);
                item.setScanTime(code.getScanTime() != null ? code.getScanTime().format(DATE_TIME_FORMAT) : null);
            } else if ("pending".equals(code.getStatus()) || "label_printed".equals(code.getStatus())) {
                pending++;
            } else if ("error".equals(code.getStatus())) {
                errors++;
                item.setHasError(true);
            }
            
            labelItems.add(item);
        }
        
        progress.setTotalLabelCount(total);
        progress.setScannedLabelCount(scanned);
        progress.setPendingLabelCount(pending);
        progress.setErrorLabelCount(errors);
        progress.setTotalQuantity(totalQty);
        progress.setScannedQuantity(scannedQty);
        progress.setPendingQuantity(totalQty.subtract(scannedQty));
        progress.setLabels(labelItems);
        progress.setCanComplete(scanned == total && errors == 0);
        progress.setHasErrors(errors > 0);
        
        return progress;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScanConfirmResult handleManualIntervention(ManualInterventionDTO intervention) {
        log.info("处理人工介入: type={}, traceCode={}", intervention.getInterventionType(), intervention.getTraceCode());
        
        MaterialTraceCode traceCode = materialTraceCodeService.getByTraceCode(intervention.getTraceCode());
        if (traceCode == null) {
            return ScanConfirmResult.failure("追溯码不存在");
        }
        
        ScanConfirmResult result = new ScanConfirmResult();
        result.setTraceCode(intervention.getTraceCode());
        result.setMaterialName(traceCode.getMaterialName());
        result.setExpectedQuantity(intervention.getExpectedQuantity());
        result.setScannedQuantity(intervention.getActualQuantity());
        
        switch (intervention.getHandlingAction()) {
            case ManualInterventionDTO.ACTION_ACCEPT_DIFFERENCE:
                traceCode.setQuantity(intervention.getActualQuantity());
                traceCode.setStatus("active");
                traceCode.setScanTime(LocalDateTime.now());
                traceCode.setUpdateTime(LocalDateTime.now());
                materialTraceCodeMapper.updateById(traceCode);
                
                result.setSuccess(true);
                result.setMessage("已接受数量差异，入库完成");
                log.info("接受数量差异: traceCode={}, adjustedQty={}", intervention.getTraceCode(), intervention.getActualQuantity());
                break;
                
            case ManualInterventionDTO.ACTION_REJECT:
                traceCode.setStatus("rejected");
                traceCode.setUpdateTime(LocalDateTime.now());
                materialTraceCodeMapper.updateById(traceCode);
                
                result.setSuccess(false);
                result.setMessage("已拒绝入库");
                log.info("拒绝入库: traceCode={}", intervention.getTraceCode());
                break;
                
            case ManualInterventionDTO.ACTION_REPRINT:
                result.setSuccess(true);
                result.setMessage("请重新打印标签");
                log.info("请求重新打印: traceCode={}", intervention.getTraceCode());
                break;
                
            case ManualInterventionDTO.ACTION_CANCEL:
                traceCode.setStatus("cancelled");
                traceCode.setUpdateTime(LocalDateTime.now());
                materialTraceCodeMapper.updateById(traceCode);
                
                result.setSuccess(false);
                result.setMessage("已取消入库");
                log.info("取消入库: traceCode={}", intervention.getTraceCode());
                break;
                
            case ManualInterventionDTO.ACTION_MANUAL_OVERRIDE:
                traceCode.setQuantity(intervention.getAdjustedQuantity());
                traceCode.setStatus("active");
                traceCode.setScanTime(LocalDateTime.now());
                traceCode.setUpdateTime(LocalDateTime.now());
                materialTraceCodeMapper.updateById(traceCode);
                
                result.setSuccess(true);
                result.setMessage("人工调整完成，入库成功");
                log.info("人工调整入库: traceCode={}, adjustedQty={}", intervention.getTraceCode(), intervention.getAdjustedQuantity());
                break;
                
            default:
                result.setSuccess(false);
                result.setMessage("未知的处理操作");
        }
        
        return result;
    }

    @Override
    public List<ManualInterventionDTO> getPendingInterventions(Long stockinId) {
        List<MaterialTraceCode> codes = materialTraceCodeService.getByStockinId(stockinId);
        List<ManualInterventionDTO> interventions = new ArrayList<>();
        
        for (MaterialTraceCode code : codes) {
            if ("error".equals(code.getStatus()) || "rejected".equals(code.getStatus())) {
                ManualInterventionDTO dto = new ManualInterventionDTO();
                dto.setStockinId(stockinId);
                dto.setTraceCode(code.getTraceCode());
                dto.setInterventionType(ManualInterventionDTO.TYPE_OTHER);
                dto.setExpectedQuantity(code.getQuantity());
                interventions.add(dto);
            }
        }
        
        return interventions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelStockin(Long stockinId, String reason) {
        log.info("取消入库单: stockinId={}, reason={}", stockinId, reason);
        
        PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
        if (stockin == null) {
            return false;
        }
        
        stockin.setStatus(99);
        // 使用setQualityRemark()存储取消原因（实体类无remark字段）
        stockin.setQualityRemark(reason);
        purchaseStockinMapper.updateById(stockin);
        
        List<MaterialTraceCode> codes = materialTraceCodeService.getByStockinId(stockinId);
        for (MaterialTraceCode code : codes) {
            code.setStatus("cancelled");
            code.setUpdateTime(LocalDateTime.now());
            materialTraceCodeMapper.updateById(code);
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeStockin(Long stockinId) {
        log.info("完成入库单: stockinId={}", stockinId);
        
        PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
        if (stockin == null) {
            return false;
        }
        
        stockin.setStatus(2);
        purchaseStockinMapper.updateById(stockin);
        
        List<MaterialTraceCode> codes = materialTraceCodeService.getByStockinId(stockinId);
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (MaterialTraceCode code : codes) {
            if ("active".equals(code.getStatus())) {
                updateInventory(stockin, code);
                // 注意：PurchaseStockin无unitPrice字段，使用totalAmount和totalQuantity计算单价
                BigDecimal unitPrice = (stockin.getTotalAmount() != null && stockin.getTotalQuantity() != null
                    && stockin.getTotalQuantity().compareTo(BigDecimal.ZERO) > 0)
                    ? new BigDecimal(stockin.getTotalAmount()).divide(stockin.getTotalQuantity(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                totalAmount = totalAmount.add(code.getQuantity() != null ?
                    code.getQuantity().multiply(unitPrice) :
                    BigDecimal.ZERO);
            }
        }

        createFinanceVoucher(stockin, totalAmount);

        log.info("入库单完成: stockinId={}, totalAmount={}", stockinId, totalAmount);
        return true;
    }

    /**
     * 更新库存：调用 InventoryService.increaseInventory 增加库存
     * 6.1.6 修复：原实现仅打 log，现实际调用 inventoryService
     */
    private void updateInventory(PurchaseStockin stockin, MaterialTraceCode code) {
        log.info("更新库存: material={}, quantity={}", code.getMaterialName(), code.getQuantity());

        try {
            InventoryIncreaseDTO increaseDTO = new InventoryIncreaseDTO();
            // materialId 在追溯码中为 String，解析为 Long
            if (code.getMaterialId() != null && !code.getMaterialId().isEmpty()) {
                try {
                    increaseDTO.setMaterialId(Long.parseLong(code.getMaterialId()));
                } catch (NumberFormatException e) {
                    log.warn("物料ID无法解析为Long，跳过库存更新: materialId={}", code.getMaterialId());
                    return;
                }
            } else {
                log.warn("物料ID为空，跳过库存更新: traceCode={}", code.getTraceCode());
                return;
            }
            increaseDTO.setWarehouseId(stockin.getWarehouseId());
            increaseDTO.setQuantity(code.getQuantity());
            // 单价（分）：使用 stockin 总金额和总数量计算
            if (stockin.getTotalAmount() != null && stockin.getTotalQuantity() != null
                    && stockin.getTotalQuantity().compareTo(BigDecimal.ZERO) > 0) {
                Long unitCostFen = BigDecimal.valueOf(stockin.getTotalAmount())
                        .divide(stockin.getTotalQuantity(), 0, RoundingMode.HALF_UP)
                        .longValue();
                increaseDTO.setUnitCost(unitCostFen);
            }
            increaseDTO.setTransactionType(1); // 1-采购入库
            increaseDTO.setBatchNo(code.getBatchNumber());
            increaseDTO.setReferenceNo(stockin.getStockinCode());
            increaseDTO.setReferenceType("purchase_stockin");

            inventoryService.increaseInventory(increaseDTO);
            log.info("库存更新成功: materialId={}, quantity={}, warehouseId={}",
                    increaseDTO.getMaterialId(), increaseDTO.getQuantity(), increaseDTO.getWarehouseId());
        } catch (Exception e) {
            log.error("库存更新失败: materialId={}, traceCode={}, 错误={}",
                    code.getMaterialId(), code.getTraceCode(), e.getMessage(), e);
            // 库存更新失败抛出，由事务回滚保证一致性
            throw new RuntimeException("库存更新失败: " + code.getMaterialName() + " - " + e.getMessage(), e);
        }
    }
    
    private void createFinanceVoucher(PurchaseStockin stockin, BigDecimal amount) {
        // 使用getStockinCode()而非getStockinNo()（新实体类字段名）
        log.info("创建财务凭证: stockinNo={}, amount={}", stockin.getStockinCode(), amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MaterialTraceCode> generateAndPrintLabels(Long stockinId, LabelPrintConfigDTO printConfig) {
        log.info("生成并打印标签: stockinId={}, peelMode={}", stockinId, printConfig.isPeelModeEnabled());
        
        PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
        if (stockin == null) {
            return new ArrayList<>();
        }
        
        MaterialTraceCodeGenerateDTO generateDTO = new MaterialTraceCodeGenerateDTO();
        // 注意：PurchaseStockin无productId/productName字段，需从入库明细获取
        // generateDTO.setMaterialId(String.valueOf(stockin.getProductId()));
        // generateDTO.setMaterialName(stockin.getProductName());
        generateDTO.setPurchaseStockinId(String.valueOf(stockinId));
        // 注意：PurchaseStockin无orderNo/supplierName字段，需从关联实体获取
        // generateDTO.setPurchaseOrderNo(stockin.getOrderNo());
        generateDTO.setSupplierId(String.valueOf(stockin.getSupplierId()));
        // generateDTO.setSupplierName(stockin.getSupplierName());
        // 注意：使用totalQuantity替代quantity（新实体类字段名）
        generateDTO.setQuantity(stockin.getTotalQuantity());
        // 注意：PurchaseStockin无unit字段
        // generateDTO.setUnit(stockin.getUnit());
        generateDTO.setWeight(stockin.getTotalQuantity());
        // generateDTO.setWeightUnit(stockin.getUnit());
        // 注意：使用warehouseId替代storeId（新实体类字段名）
        generateDTO.setStoreId(String.valueOf(stockin.getWarehouseId()));
        generateDTO.setEntryType("order");
        generateDTO.setGenerateCount(1);
        
        List<MaterialTraceCode> codes = materialTraceCodeService.generateBatch(generateDTO);
        
        for (MaterialTraceCode code : codes) {
            code.setStatus("label_printed");
            code.setUpdateTime(LocalDateTime.now());
            materialTraceCodeMapper.updateById(code);
        }
        
        if (printConfig != null && printConfig.isPeelModeEnabled()) {
            materialLabelPrintService.batchPrintMaterialLabelsWithPeelMode(codes, printConfig);
        } else {
            materialLabelPrintService.batchPrintMaterialLabels(codes);
        }
        
        return codes;
    }
}
