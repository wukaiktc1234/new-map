package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.MaterialTraceCodeGenerateDTO;
import com.foodtraceability.dto.PrintTaskDTO;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.entity.StoreInventoryLog;
import com.foodtraceability.mapper.MaterialTraceCodeMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.MaterialTraceCodeService;
import com.foodtraceability.service.PrintService;
import com.foodtraceability.service.PurchaseStockinService;
import com.foodtraceability.service.StoreInventoryLogService;
import com.foodtraceability.entity.PurchaseStockin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MaterialTraceCodeServiceImpl extends ServiceImpl<MaterialTraceCodeMapper, MaterialTraceCode> implements MaterialTraceCodeService {

    private static final Logger log = LoggerFactory.getLogger(MaterialTraceCodeServiceImpl.class);


    public MaterialTraceCodeServiceImpl(MaterialTraceCodeMapper materialTraceCodeMapper, PrintService printService, @Nullable InventoryService inventoryService, @Nullable StoreInventoryLogService storeInventoryLogService, @Nullable PurchaseStockinService purchaseStockinService) {
        this.materialTraceCodeMapper = materialTraceCodeMapper;
        this.printService = printService;
        this.inventoryService = inventoryService;
        this.storeInventoryLogService = storeInventoryLogService;
        this.purchaseStockinService = purchaseStockinService;
    }

    private final MaterialTraceCodeMapper materialTraceCodeMapper;

    private final InventoryService inventoryService;

    private final StoreInventoryLogService storeInventoryLogService;

    private final PurchaseStockinService purchaseStockinService;

    private static final String PREFIX = "MTC";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public List<MaterialTraceCode> generateBatch(MaterialTraceCodeGenerateDTO dto) {
        List<MaterialTraceCode> codes = new ArrayList<>();
        int generateCount = dto.getGenerateCount() != null ? dto.getGenerateCount() : 1;
        
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        String batchId = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        BigDecimal totalWeight = dto.getWeight() != null ? dto.getWeight() : BigDecimal.ZERO;
        BigDecimal weightPerItem = totalWeight.divide(new BigDecimal(generateCount), 3, BigDecimal.ROUND_HALF_UP);
        
        for (int i = 0; i < generateCount; i++) {
            MaterialTraceCode code = new MaterialTraceCode();
            
            String traceCodeId = PREFIX + dateStr + String.format("%04d", getNextSequence());
            String traceCode = traceCodeId + batchId.substring(0, 3);
            
            code.setTraceCodeId(traceCodeId);
            code.setTraceCode(traceCode);
            code.setMaterialId(dto.getMaterialId());
            code.setMaterialName(dto.getMaterialName());
            code.setBatchNumber(dto.getBatchNumber());
            code.setSupplierId(dto.getSupplierId());
            code.setSupplierName(dto.getSupplierName());
            code.setPurchaseOrderId(dto.getPurchaseStockinId());
            code.setPurchaseOrderNo(dto.getPurchaseOrderNo());
            code.setProductionDate(dto.getProductionDate());
            code.setShelfLifeDays(dto.getShelfLifeDays());
            
            if (dto.getProductionDate() != null && dto.getShelfLifeDays() != null) {
                code.setExpiryDate(dto.getProductionDate().plusDays(dto.getShelfLifeDays()));
            }
            
            if (dto.getInboundTime() != null) {
                code.setGenerateTime(LocalDateTime.parse(dto.getInboundTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                code.setGenerateTime(LocalDateTime.now());
            }
            
            if (dto.getQuantity() != null && generateCount > 0) {
                code.setQuantity(dto.getQuantity().divide(new BigDecimal(generateCount), 3, BigDecimal.ROUND_HALF_UP));
            }
            code.setUnit(dto.getUnit());
            code.setWeight(weightPerItem);
            code.setWeightUnit(dto.getWeightUnit());
            code.setStorageLocation(dto.getStorageLocation());
            code.setStorageCondition(dto.getStorageCondition());
            code.setStoreId(dto.getStoreId());
            code.setStoreName(dto.getStoreName());
            code.setEntryType(dto.getEntryType() != null ? dto.getEntryType() : "quick");
            code.setStatus("pending");
            code.setCreateTime(LocalDateTime.now());
            code.setUpdateTime(LocalDateTime.now());
            
            save(code);
            codes.add(code);
        }
        
        log.info("生成原料追溯码完成: 共{}条, 状态=pending(待入库确认)", codes.size());
        
        return codes;
    }

    private void updateStoreInventory(MaterialTraceCodeGenerateDTO dto, BigDecimal totalWeight) {
        if (dto.getStoreId() == null || dto.getStoreId().isEmpty()) {
            log.info("未指定门店ID，跳过库存更新");
            return;
        }
        
        try {
            Long storeIdLong = null;
            try {
                storeIdLong = Long.parseLong(dto.getStoreId());
            } catch (NumberFormatException e) {
                log.warn("门店ID格式不正确: {}", dto.getStoreId());
                return;
            }
            
            if (inventoryService != null) {
                Inventory inventory = new Inventory();
                inventory.setProductName(dto.getMaterialName());
                inventory.setStoreId(storeIdLong);
                inventory.setCurrentStock(totalWeight);
                inventory.setUnit(dto.getWeightUnit() != null ? dto.getWeightUnit() : "kg");
                inventory.setInventoryType(2);
                inventory.setCreatedAt(new java.util.Date());
                inventory.setUpdatedAt(new java.util.Date());
                
                log.info("创建门店库存记录: storeId={}, productName={}, quantity={}", 
                    storeIdLong, dto.getMaterialName(), totalWeight);
            }
            
            if (storeInventoryLogService != null) {
                StoreInventoryLog logEntry = new StoreInventoryLog();
                logEntry.setStoreId(dto.getStoreId());
                logEntry.setStoreName(dto.getStoreName());
                logEntry.setProductName(dto.getMaterialName());
                logEntry.setType(1);
                logEntry.setChangeQuantity(totalWeight);
                logEntry.setBeforeStock(BigDecimal.ZERO);
                logEntry.setAfterStock(totalWeight);
                logEntry.setRemark("原料入库 - " + getEntryTypeLabel(dto.getEntryType()));
                
                storeInventoryLogService.createLog(logEntry);
                log.info("创建库存变动日志: storeId={}, productName={}, quantity={}", 
                    dto.getStoreId(), dto.getMaterialName(), totalWeight);
            }
        } catch (Exception e) {
            log.error("更新门店库存失败: {}", e.getMessage(), e);
        }
    }

    private String getEntryTypeLabel(String entryType) {
        if (entryType == null) return "快速入库";
        switch (entryType) {
            case "quick": return "快速入库（自采）";
            case "supplier": return "供应商直送";
            case "order": return "采购订单";
            default: return entryType;
        }
    }

    @Override
    public MaterialTraceCode getByTraceCode(String traceCode) {
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTraceCode::getTraceCode, traceCode);
        return getOne(wrapper);
    }

    @Override
    public List<MaterialTraceCode> getByStockinId(Long stockinId) {
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTraceCode::getPurchaseOrderId, String.valueOf(stockinId));
        return list(wrapper);
    }

    @Override
    public List<MaterialTraceCode> getByProductId(Long productId) {
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTraceCode::getMaterialId, String.valueOf(productId));
        return list(wrapper);
    }

    @Override
    @Transactional
    public boolean updateStatus(String traceCodeId, String newStatus, String operatorName, String reason) {
        MaterialTraceCode code = getByTraceCode(traceCodeId);
        if (code == null) {
            return false;
        }
        
        code.setStatus(newStatus);
        code.setUpdateTime(LocalDateTime.now());
        
        return updateById(code);
    }

    @Override
    @Transactional
    public boolean pickTraceCode(String traceCode, Long usedById, String usedByName, String usagePurpose) {
        MaterialTraceCode code = getByTraceCode(traceCode);
        if (code == null || !"active".equals(code.getStatus())) {
            return false;
        }
        
        code.setStatus("picked");
        code.setUpdateTime(LocalDateTime.now());
        
        return updateById(code);
    }

    @Override
    @Transactional
    public boolean useTraceCode(String traceCode, String foodTraceCode, Long usedById, String usedByName) {
        MaterialTraceCode code = getByTraceCode(traceCode);
        if (code == null || !"picked".equals(code.getStatus())) {
            return false;
        }
        
        code.setStatus("used");
        code.setUseTime(LocalDateTime.now());
        code.setUpdateTime(LocalDateTime.now());
        
        return updateById(code);
    }

    @Override
    @Transactional
    public boolean returnTraceCode(String traceCode, String reason) {
        MaterialTraceCode code = getByTraceCode(traceCode);
        if (code == null) {
            return false;
        }
        
        code.setStatus("returned");
        code.setUpdateTime(LocalDateTime.now());
        
        return updateById(code);
    }

    @Override
    public List<MaterialTraceCode> getExpiredCodes() {
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(MaterialTraceCode::getExpiryDate, LocalDate.now())
               .ne(MaterialTraceCode::getStatus, "expired");
        return list(wrapper);
    }

    @Override
    public List<MaterialTraceCode> getExpiringSoonCodes(int days) {
        LocalDate targetDate = LocalDate.now().plusDays(days);
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(MaterialTraceCode::getExpiryDate, LocalDate.now(), targetDate)
               .eq(MaterialTraceCode::getStatus, "active");
        return list(wrapper);
    }

    @Override
    @Transactional
    public int markExpiredCodes() {
        List<MaterialTraceCode> expiredCodes = getExpiredCodes();
        int count = 0;
        for (MaterialTraceCode code : expiredCodes) {
            code.setStatus("expired");
            code.setUpdateTime(LocalDateTime.now());
            if (updateById(code)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String generateQrCode(String traceCodeId) {
        MaterialTraceCode code = getById(traceCodeId);
        if (code == null) {
            return null;
        }
        return "/api/v1/material-trace-code/qr/" + code.getTraceCode();
    }

    private final PrintService printService;

    @Override
    public boolean batchPrint(List<String> traceCodeIds, Long printerId) {
        log.info("batchPrint called with traceCodeIds: {}, printerId: {}", traceCodeIds, printerId);
        
        if (traceCodeIds == null || traceCodeIds.isEmpty()) {
            log.warn("traceCodeIds is null or empty");
            return false;
        }
        
        for (String traceCodeId : traceCodeIds) {
            log.info("Processing traceCodeId: {}", traceCodeId);
            
            // 使用 traceCodeId 字段查询，而不是 id
            LambdaQueryWrapper<MaterialTraceCode> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MaterialTraceCode::getTraceCodeId, traceCodeId);
            MaterialTraceCode code = getOne(queryWrapper);
            
            if (code == null) {
                log.warn("Code not found for traceCodeId: {}", traceCodeId);
                continue;
            }
            
            PrintTaskDTO task = new PrintTaskDTO();
            task.setTaskName("追溯码打印-" + code.getMaterialName());
            task.setTaskType("TRACE_CODE");
            String numericPart = traceCodeId.replaceAll("[^0-9]", "");
            task.setBusinessId(numericPart.isEmpty() ? System.currentTimeMillis() : Long.parseLong(numericPart.substring(0, Math.min(numericPart.length(), 18))));
            task.setBusinessType("MATERIAL_TRACE_CODE");
            task.setPrinterName(printerId != null ? "PRINTER_" + printerId : "DEFAULT");
            task.setPriority(1);
            task.setStatus("PENDING");
            
            PrintTaskDTO addedTask = printService.addPrintTask(task);
            log.info("Print task added: {}, taskName: {}", addedTask.getId(), addedTask.getTaskName());
        }
        
        return true;
    }

    @Override
    public int countByStatus(String status) {
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTraceCode::getStatus, status);
        return (int) count(wrapper);
    }

    private int getNextSequence() {
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(MaterialTraceCode::getTraceCodeId, PREFIX + LocalDate.now().format(DATE_FORMAT))
               .orderByDesc(MaterialTraceCode::getTraceCodeId)
               .last("LIMIT 1");
        MaterialTraceCode lastCode = getOne(wrapper);
        if (lastCode == null) {
            return 1;
        }
        String lastId = lastCode.getTraceCodeId();
        String seqStr = lastId.substring(lastId.length() - 4);
        return Integer.parseInt(seqStr) + 1;
    }

    @Override
    @Transactional
    public boolean confirmStockin(String traceCode, String operatorName) {
        MaterialTraceCode code = getByTraceCode(traceCode);
        if (code == null) {
            log.warn("追溯码不存在: {}", traceCode);
            return false;
        }
        
        if (!"pending".equals(code.getStatus())) {
            log.warn("追溯码状态不是待入库，无法确认入库: traceCode={}, status={}", traceCode, code.getStatus());
            return false;
        }
        
        code.setStatus("in_stock");
        code.setScanTime(LocalDateTime.now());
        code.setUpdateTime(LocalDateTime.now());
        updateById(code);
        
        log.info("追溯码确认入库成功: traceCode={}, materialName={}", traceCode, code.getMaterialName());
        
        updateInventoryAfterConfirm(code);
        
        createPurchaseStockinRecord(code, operatorName);
        
        return true;
    }

    @Override
    @Transactional
    public int batchConfirmStockin(List<String> traceCodes, String operatorName) {
        int successCount = 0;
        for (String traceCode : traceCodes) {
            if (confirmStockin(traceCode, operatorName)) {
                successCount++;
            }
        }
        log.info("批量确认入库完成: 总数={}, 成功={}", traceCodes.size(), successCount);
        return successCount;
    }

    private void updateInventoryAfterConfirm(MaterialTraceCode code) {
        if (code.getStoreId() == null || code.getStoreId().isEmpty()) {
            log.info("未指定门店ID，跳过库存更新");
            return;
        }
        
        try {
            Long storeIdLong = null;
            try {
                storeIdLong = Long.parseLong(code.getStoreId());
            } catch (NumberFormatException e) {
                log.warn("门店ID格式不正确: {}", code.getStoreId());
                return;
            }
            
            BigDecimal quantity = code.getWeight() != null ? code.getWeight() : BigDecimal.ZERO;
            
            if (inventoryService != null) {
                Inventory inventory = new Inventory();
                inventory.setProductName(code.getMaterialName());
                inventory.setStoreId(storeIdLong);
                inventory.setCurrentStock(quantity);
                inventory.setUnit(code.getWeightUnit() != null ? code.getWeightUnit() : "kg");
                inventory.setInventoryType(2);
                inventory.setCreatedAt(new java.util.Date());
                inventory.setUpdatedAt(new java.util.Date());
                
                log.info("确认入库-更新门店库存: storeId={}, productName={}, quantity={}", 
                    storeIdLong, code.getMaterialName(), quantity);
            }
            
            if (storeInventoryLogService != null) {
                StoreInventoryLog logEntry = new StoreInventoryLog();
                logEntry.setStoreId(code.getStoreId());
                logEntry.setStoreName(code.getStoreName());
                logEntry.setProductName(code.getMaterialName());
                logEntry.setType(1);
                logEntry.setChangeQuantity(quantity);
                logEntry.setBeforeStock(BigDecimal.ZERO);
                logEntry.setAfterStock(quantity);
                logEntry.setRemark("扫码确认入库 - " + getEntryTypeLabel(code.getEntryType()));
                
                storeInventoryLogService.createLog(logEntry);
                log.info("确认入库-创建库存变动日志: storeId={}, productName={}, quantity={}", 
                    code.getStoreId(), code.getMaterialName(), quantity);
            }
        } catch (Exception e) {
            log.error("确认入库-更新门店库存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建采购入库记录
     * 适配新实体类 PurchaseStockin 的字段规范（2026-04-25重构）
     * 字段映射说明：
     * - stockinCode: 入库单编号（原 stockinNo）
     * - totalQuantity: 总数量（原 quantity）
     * - qualityCheckResult: 质检结果整数编码（原 qualityCheck字符串）
     * - createUserId: 创建人ID长整型（原 createdBy字符串）
     * - stockinDate: LocalDate类型（原 Date类型）
     */
    private void createPurchaseStockinRecord(MaterialTraceCode code, String operatorName) {
        if (purchaseStockinService == null) {
            log.warn("采购入库服务未注入，跳过创建采购入库记录");
            return;
        }

        try {
            PurchaseStockin stockin = new PurchaseStockin();
            // 入库单编号：使用时间戳生成唯一编号
            stockin.setStockinCode("SI" + System.currentTimeMillis());
            // 供应商ID：从追溯码的供应商ID转换
            stockin.setSupplierId(code.getSupplierId() != null ? Long.parseLong(code.getSupplierId()) : null);
            // 总数量：使用追溯码的重量
            stockin.setTotalQuantity(code.getWeight() != null ? code.getWeight() : BigDecimal.ZERO);
            // 入库日期：使用当前日期（LocalDate类型）
            stockin.setStockinDate(LocalDate.now());
            // 入库类型：2-扫码确认入库（使用setStockinType而非setStockType）
            stockin.setStockinType(2);
            // 状态：1-已入库
            stockin.setStatus(1);
            // 质检结果：1-合格（默认扫码确认视为合格）
            stockin.setQualityCheckResult(1);
            // 质检备注：记录来源信息
            stockin.setQualityRemark("扫码确认入库自动质检通过");
            // 创建人ID：如果operatorName是数字则解析为ID，否则使用系统默认值
            if (operatorName != null && operatorName.matches("\\d+")) {
                stockin.setCreateUserId(Long.parseLong(operatorName));
            } else {
                stockin.setCreateUserId(1L); // 系统默认用户ID
            }
            // 备注：记录追溯码信息便于溯源
            stockin.setQualityRemark("扫码确认入库 - 追溯码: " + code.getTraceCode());

            // 使用MyBatis-Plus的save方法保存（替代原createPurchaseStockin方法）
            purchaseStockinService.save(stockin);

            log.info("确认入库-创建采购入库记录成功: traceCode={}, stockinCode={}",
                code.getTraceCode(), stockin.getStockinCode());
        } catch (Exception e) {
            log.error("确认入库-创建采购入库记录失败: {}", e.getMessage(), e);
        }
    }
}
