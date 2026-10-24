package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.MaterialTraceCodeGenerateDTO;
import com.foodtraceability.dto.PrintTaskDTO;
import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.entity.StoreInventoryLog;
import com.foodtraceability.mapper.MaterialArchiveMapper;
import com.foodtraceability.mapper.MaterialTraceCodeMapper;
import com.foodtraceability.mapper.PurchaseStockinItemMapper;
import com.foodtraceability.service.InventoryService;
import com.foodtraceability.service.MaterialTraceCodeService;
import com.foodtraceability.service.PrintService;
import com.foodtraceability.service.PurchaseStockinService;
import com.foodtraceability.service.StoreInventoryLogService;
import com.foodtraceability.service.purchase.StockinLocationResolver;
import com.foodtraceability.entity.MaterialArchive;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.PurchaseStockinItem;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.event.EventPublisher;
import com.foodtraceability.event.PurchaseStockInEvent;
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


    public MaterialTraceCodeServiceImpl(MaterialTraceCodeMapper materialTraceCodeMapper, PrintService printService,
                                        @Nullable InventoryService inventoryService,
                                        @Nullable StoreInventoryLogService storeInventoryLogService,
                                        @Nullable PurchaseStockinService purchaseStockinService,
                                        EventPublisher eventPublisher,
                                        MaterialArchiveMapper materialArchiveMapper,
                                        PurchaseStockinItemMapper purchaseStockinItemMapper,
                                        StockinLocationResolver stockinLocationResolver) {
        this.materialTraceCodeMapper = materialTraceCodeMapper;
        this.printService = printService;
        this.inventoryService = inventoryService;
        this.storeInventoryLogService = storeInventoryLogService;
        this.purchaseStockinService = purchaseStockinService;
        this.eventPublisher = eventPublisher;
        this.materialArchiveMapper = materialArchiveMapper;
        this.purchaseStockinItemMapper = purchaseStockinItemMapper;
        this.stockinLocationResolver = stockinLocationResolver;
    }

    private final MaterialTraceCodeMapper materialTraceCodeMapper;

    private final InventoryService inventoryService;

    private final StoreInventoryLogService storeInventoryLogService;

    private final PurchaseStockinService purchaseStockinService;

    /** 采购入库完成事件发布器（DATA-007：扫码确认入库也要联动成本） */
    private final EventPublisher eventPublisher;

    /** 物料主档（material_archives）：单价解析链最后一级来源 reference_price（单位：分） */
    private final MaterialArchiveMapper materialArchiveMapper;

    /** 入库明细（purchase_stockin_items）：单价解析链第二级来源 unit_price（单位：分） */
    private final PurchaseStockinItemMapper purchaseStockinItemMapper;

    /** 入库库位解析（整改 1：ID → ID，五级优先序，绝不返回 null） */
    private final StockinLocationResolver stockinLocationResolver;

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
        return confirmStockin(traceCode, operatorName, null, null);
    }

    /**
     * 扫码确认入库（整改 1）：强制解析库位与单价，强制写 purchase_stockin_items。
     *
     * @param explicitLocationId 显式入库库位（可空；空则走 StockinLocationResolver 五级链）
     * @param explicitUnitPriceFen 显式单价（分，可空；空则走单价解析链，仍为空则拒绝确认）
     */
    @Override
    @Transactional
    public boolean confirmStockin(String traceCode, String operatorName,
                                  Long explicitLocationId, Long explicitUnitPriceFen) {
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
        // 假成功修复：原来忽略 updateById 的 boolean，状态没改成也继续往下走并返回 true。
        if (!updateById(code)) {
            log.error("追溯码确认入库失败：状态更新未生效, traceCode={}", traceCode);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "确认入库失败：追溯码状态更新未生效（traceCode=" + traceCode + "）");
        }
        
        // 整改 1：库位与单价都必须解析出来，否则拒绝确认（不再软匹配/不写 0）
        Long orderId = parseLongOrNull(code.getPurchaseOrderId());
        Long locationId = stockinLocationResolver.resolveLocationId(explicitLocationId, null, orderId, code.getStoreId());
        Long unitPriceFen = explicitUnitPriceFen != null && explicitUnitPriceFen > 0
                ? explicitUnitPriceFen
                : resolveUnitPriceFen(code, null);
        if (unitPriceFen == null || unitPriceFen <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "无法确定入库单价：请指定 unitPrice 或维护物料参考价");
        }

        // 物料ID必须可解析：脏数据一律拒绝，且必须在建单/写明细之前拦下（避免部分写入）
        Long materialId = parseLongOrNull(code.getMaterialId());
        if (materialId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "确认入库失败：物料ID非法（materialId=" + code.getMaterialId()
                            + "，traceCode=" + code.getTraceCode() + "）");
        }

        log.info("追溯码确认入库成功: traceCode={}, materialName={}, locationId={}, unitPriceFen={}, materialId={}",
                traceCode, code.getMaterialName(), locationId, unitPriceFen, materialId);

        // 顺序：建入库单（含 location/order/明细）→ 落统一库存账（用单号做 sourceRef）→ 发事件（AFTER_COMMIT 联动成本）
        PurchaseStockin stockin = createPurchaseStockinRecord(code, operatorName, locationId, orderId,
                unitPriceFen, materialId);

        updateInventoryAfterConfirm(code, stockin.getStockinCode(), locationId, unitPriceFen, materialId);

        publishTraceConfirmStockinEvent(stockin, code);

        return true;
    }

    @Override
    @Transactional
    public int batchConfirmStockin(List<String> traceCodes, String operatorName) {
        int successCount = 0;
        List<String> failedCodes = new ArrayList<>();
        for (String traceCode : traceCodes) {
            try {
                if (confirmStockin(traceCode, operatorName)) {
                    successCount++;
                } else {
                    failedCodes.add(traceCode);
                }
            } catch (Exception e) {
                // 保持批量「尽力而为」语义，但失败必须逐条留痕并汇总，不能被算作成功。
                failedCodes.add(traceCode);
                log.error("批量确认入库-单条失败: traceCode={}, error={}", traceCode, e.getMessage(), e);
            }
        }
        if (!failedCodes.isEmpty()) {
            log.error("批量确认入库存在失败: 总数={}, 成功={}, 失败{}条={}",
                    traceCodes.size(), successCount, failedCodes.size(), failedCodes);
        } else {
            log.info("批量确认入库完成: 总数={}, 成功={}", traceCodes.size(), successCount);
        }
        return successCount;
    }

    /**
     * 扫码确认入库 → 写统一库存账（location 维度），并保留 legacy store_inventory_log 审计。
     *
     * <p>DATA-007 修复要点：</p>
     * <ul>
     *   <li>原实现只 {@code new Inventory()} 填字段 + 打日志，<b>从不落库</b>（假成功）；现走
     *       {@link InventoryService#increaseStockAtLocation}（统一账唯一写路径，写 inventory 行 + inventory_movement）。</li>
     *   <li>storeId → locationId 走 {@link LocationIdBridge}（stores_new 口径）；解析不到<b>抛错回滚</b>，
     *       不再"跳过 + 返回 true"。</li>
     *   <li>unitCost：追溯码上没有单价，传 null 交由加权平均处理，<b>不臆造 0</b>。</li>
     *   <li>legacy 日志的 before/after 改成真实库存（入账前查统一账，取不到按 0 + 本次增量）。</li>
     *   <li>方法内不再捕获异常：任何失败都必须让 confirmStockin 的既有事务回滚。</li>
     * </ul>
     *
     * @param code      追溯码
     * @param sourceRef 来源单据号（入库单号），写入 inventory_movement.source_ref
     */
    private void updateInventoryAfterConfirm(MaterialTraceCode code, String sourceRef,
                                             Long locationId, Long unitPriceFen, Long materialId) {
        if (inventoryService == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "确认入库失败：库存服务未注入（traceCode=" + code.getTraceCode() + "）");
        }
        if (code.getStoreId() == null || code.getStoreId().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "确认入库失败：追溯码缺少门店（traceCode=" + code.getTraceCode() + "）");
        }

        // 整改 1：库位已由 StockinLocationResolver 统一按 ID→ID 解析（禁止字符串软匹配/数值兜底），
        // 这里只做非空校验，不再自行换算 store_id。
        if (locationId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, StockinLocationResolver.RESOLVE_FAILED_MESSAGE);
        }

        // materialId 已由 confirmStockin 早期解析（此处保证非空）
        if (materialId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "确认入库失败：物料ID非法（traceCode=" + code.getTraceCode() + "）");
        }

        BigDecimal quantity = code.getWeight() != null ? code.getWeight() : code.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "确认入库失败：入库数量为空或非正数（traceCode=" + code.getTraceCode() + "）");
        }

        String unit = code.getWeightUnit() != null && !code.getWeightUnit().isBlank()
                ? code.getWeightUnit() : "kg";

        // legacy 审计日志的真实 before/after：入账前先读统一账
        Inventory before = inventoryService.getByLocationAndMaterial(locationId, materialId);
        BigDecimal beforeQty = before != null && before.getQuantity() != null
                ? before.getQuantity() : BigDecimal.ZERO;

        inventoryService.increaseStockAtLocation(
                locationId,
                materialId,
                code.getMaterialName(),
                quantity,
                unit,
                unitPriceFen,               // unitCost：整改 1 起单价必填（分），参与加权平均
                code.getBatchNumber(),      // batchNo
                "TRACE_CONFIRM_STOCKIN",    // sourceType（S5 词表）
                sourceRef);

        log.info("确认入库-统一库存账已入账: locationId={}, materialId={}, quantity={}{}, batchNo={}, sourceRef={}",
                locationId, materialId, quantity, unit, code.getBatchNumber(), sourceRef);

        if (storeInventoryLogService != null) {
            StoreInventoryLog logEntry = new StoreInventoryLog();
            // StoreInventoryLogService 要求 productId 非空（= 统一账 materialId），否则直接拒绝写日志
            logEntry.setProductId(materialId);
            logEntry.setStoreId(code.getStoreId());
            logEntry.setStoreName(code.getStoreName());
            logEntry.setProductName(code.getMaterialName());
            logEntry.setType(1);
            logEntry.setChangeQuantity(quantity);
            logEntry.setBeforeStock(beforeQty);
            logEntry.setAfterStock(beforeQty.add(quantity));
            logEntry.setRemark("扫码确认入库 - " + getEntryTypeLabel(code.getEntryType()) + " - 入库单:" + sourceRef);

            storeInventoryLogService.createLog(logEntry);
            log.info("确认入库-legacy 库存日志已写（审计用）: storeId={}, productName={}, before={}, after={}",
                    code.getStoreId(), code.getMaterialName(), beforeQty, beforeQty.add(quantity));
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
    private PurchaseStockin createPurchaseStockinRecord(MaterialTraceCode code, String operatorName,
                                                       Long locationId, Long orderId, Long unitPriceFen,
                                                       Long materialId) {
        if (purchaseStockinService == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "确认入库失败：采购入库服务未注入（traceCode=" + code.getTraceCode() + "）");
        }

        {
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

            // 整改 1：库位归属只走 ID（resolver 结果，非空；列 NOT NULL），采购单关联能推出来就写上
            if (locationId == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, StockinLocationResolver.RESOLVE_FAILED_MESSAGE);
            }
            stockin.setLocationId(locationId);
            stockin.setOrderId(orderId);
            BigDecimal quantity = code.getWeight() != null ? code.getWeight() : code.getQuantity();
            if (quantity != null && unitPriceFen != null) {
                // total_amount 存分（bigint）
                stockin.setTotalAmount(quantity.multiply(BigDecimal.valueOf(unitPriceFen))
                        .setScale(0, java.math.RoundingMode.HALF_UP).longValue());
            }

            // 使用MyBatis-Plus的save方法保存（替代原createPurchaseStockin方法）
            if (!purchaseStockinService.save(stockin) || stockin.getStockinId() == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "确认入库失败：采购入库单保存失败（traceCode=" + code.getTraceCode() + "）");
            }

            log.info("确认入库-创建采购入库记录成功: traceCode={}, stockinId={}, stockinCode={}, locationId={}, orderId={}",
                code.getTraceCode(), stockin.getStockinId(), stockin.getStockinCode(), locationId, orderId);

            // 整改 1：强制写明细（purchase_stockin_items），location_id 必须是 resolver 的 ID 结果
            createStockinItem(stockin, code, locationId, unitPriceFen, quantity, materialId);
            return stockin;
        }
    }

    /**
     * 写入库明细（整改 1：扫码确认也必须写 purchase_stockin_items，且 location_id 非空）。
     *
     * <p>字段口径：actual_quantity=追溯码重量；unit_price=单价（分）；amount=数量×单价（分）；
     * location_id=StockinLocationResolver 的 ID 结果（禁止字符串软匹配）；order_item_id 扫码确认场景为空。</p>
     */
    private void createStockinItem(PurchaseStockin stockin, MaterialTraceCode code,
                                   Long locationId, Long unitPriceFen, BigDecimal quantity,
                                   Long materialId) {
        if (purchaseStockinItemMapper == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "确认入库失败：入库明细 Mapper 未注入（traceCode=" + code.getTraceCode() + "）");
        }
        if (locationId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, StockinLocationResolver.RESOLVE_FAILED_MESSAGE);
        }
        if (unitPriceFen == null || unitPriceFen <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "无法确定入库单价：请指定 unitPrice 或维护物料参考价");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "确认入库失败：入库数量为空或非正数（traceCode=" + code.getTraceCode() + "）");
        }

        PurchaseStockinItem item = new PurchaseStockinItem();
        item.setStockinId(stockin.getStockinId());
        item.setOrderItemId(null);   // 扫码确认无采购订单行
        item.setMaterialId(materialId);
        item.setMaterialName(code.getMaterialName());
        item.setBatchNo(code.getBatchNumber());
        item.setActualQuantity(quantity);
        item.setUnit(code.getWeightUnit() != null && !code.getWeightUnit().isBlank()
                ? code.getWeightUnit() : "kg");
        item.setUnitPrice(unitPriceFen);
        item.setAmount(quantity.multiply(BigDecimal.valueOf(unitPriceFen))
                .setScale(0, java.math.RoundingMode.HALF_UP).longValue());
        item.setLocationId(locationId);
        item.setRemark("扫码确认入库 - 追溯码:" + code.getTraceCode());
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        item.setDeleted(0);

        purchaseStockinItemMapper.insert(item);
        log.info("确认入库-已写入库明细: stockinId={}, materialId={}, qty={}, unitPrice={}分, amount={}分, locationId={}",
                stockin.getStockinId(), item.getMaterialId(), item.getActualQuantity(),
                item.getUnitPrice(), item.getAmount(), item.getLocationId());
    }

    /**
     * 发布「扫码确认入库」的采购入库完成事件（DATA-007）。
     *
     * <p>为什么必须发：成本联动（cost_records）挂在 PurchaseStockInEventListener 上，
     * 原实现只 save 入库单、从不发事件 → 扫码入库的成本永远不入账。</p>
     *
     * <p>为什么带 sourceType=TRACE_CONFIRM：追溯码已经存在，监听器据此跳过
     * generateMaterialTraceCodes，避免「确认一张码又生成一张新码」的递归。</p>
     *
     * <p>发布走 EventPublisher（AFTER_COMMIT 语义：主事务提交后由监听器执行），
     * 与采购入库单确认链路保持一致。</p>
     */
    private void publishTraceConfirmStockinEvent(PurchaseStockin stockin, MaterialTraceCode code) {
        if (eventPublisher == null) {
            log.warn("事件发布器未注入，跳过采购入库完成事件: stockinCode={}", stockin.getStockinCode());
            return;
        }
        PurchaseStockInEvent event = new PurchaseStockInEvent();
        event.setSourceType(PurchaseStockInEvent.SOURCE_TRACE_CONFIRM);
        event.setStockinId(stockin.getStockinId());
        event.setStockinNo(stockin.getStockinCode());
        event.setSupplierId(stockin.getSupplierId());
        event.setSupplierName(code.getSupplierName());
        event.setProductId(parseLongOrNull(code.getMaterialId()));
        event.setProductName(code.getMaterialName());
        event.setQuantity(code.getWeight() != null ? code.getWeight() : code.getQuantity());
        event.setUnit(code.getWeightUnit());
        event.setBatchNo(code.getBatchNumber());
        event.setStockinTime(LocalDateTime.now());
        event.setOperatorName(null);

        // 单价解析链（DATA-007 收尾）：a) 追溯码自身 → b) 入库明细 → c) 物料主档参考价 → d) 缺失则 null + WARN
        Long unitPriceFen = resolveUnitPriceFen(code, stockin);
        BigDecimal quantity = code.getWeight() != null ? code.getWeight() : code.getQuantity();
        if (unitPriceFen != null) {   // confirmStockin 已强校验非空；此处保留防御分支
            // 事件里的金额/单价单位是「元」，库里是「分」
            event.setUnitPrice(BigDecimal.valueOf(unitPriceFen).divide(BigDecimal.valueOf(100L), 2, java.math.RoundingMode.HALF_UP));
            if (quantity != null) {
                event.setTotalAmount(BigDecimal.valueOf(unitPriceFen).multiply(quantity)
                        .divide(BigDecimal.valueOf(100L), 2, java.math.RoundingMode.HALF_UP));
            } else {
                event.setTotalAmount(null);
                log.warn("扫码确认入库-缺少数量，成本金额无法计算: traceCode={}, materialId={}",
                        code.getTraceCode(), code.getMaterialId());
            }
        } else {
            // 三处都取不到单价：不臆造 0（历史缺陷就是把「漏账」换成「金额 0 的错账」）
            event.setTotalAmount(null);
            log.warn("扫码确认入库-单价缺失，成本行以未知金额记录: traceCode={}, materialId={}",
                    code.getTraceCode(), code.getMaterialId());
        }
        if (code.getStoreId() != null && code.getStoreId().matches("\\d+")) {
            event.setStoreId(Long.parseLong(code.getStoreId()));
        }
        event.setStoreName(code.getStoreName());
        eventPublisher.publishPurchaseStockInEvent(event);
        log.info("确认入库-已发布采购入库完成事件（sourceType={}）: stockinId={}, stockinCode={}",
                event.getSourceType(), event.getStockinId(), event.getStockinNo());
    }

    /**
     * 单价解析链（单位：分）。
     *
     * <ol>
     *   <li>a) 追溯码自身：{@code material_trace_code} 表当前<b>没有单价列</b>（实体也无该字段），
     *       故此级在当前模型下恒为空；保留位置以便后续加列时直接接入。</li>
     *   <li>b) 本次入库单明细：{@code purchase_stockin_items.unit_price}（本次刚创建的入库单）。</li>
     *   <li>c) 物料主档参考价：{@code material_archives.reference_price}（bigint，分）。</li>
     *   <li>d) 都没有 → 返回 null（调用方写 null 金额 + WARN，<b>绝不落 0</b>）。</li>
     * </ol>
     *
     * @return 单价（分）；解析不到返回 null
     */
    private Long resolveUnitPriceFen(MaterialTraceCode code, PurchaseStockin stockin) {
        // a) 追溯码自身单价：当前表结构无该列（见 javadoc），跳过
        // b) 入库明细 unit_price
        if (stockin != null && stockin.getStockinId() != null && purchaseStockinItemMapper != null) {
            try {
                List<PurchaseStockinItem> items = purchaseStockinItemMapper.selectList(
                        new LambdaQueryWrapper<PurchaseStockinItem>()
                                .eq(PurchaseStockinItem::getStockinId, stockin.getStockinId())
                                .orderByAsc(PurchaseStockinItem::getStockinItemId));
                if (items != null) {
                    for (PurchaseStockinItem item : items) {
                        if (item.getUnitPrice() != null && item.getUnitPrice() > 0) {
                            return item.getUnitPrice();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("扫码确认入库-读取入库明细单价失败，继续降级: stockinId={}, error={}",
                        stockin.getStockinId(), e.getMessage());
            }
        }

        // c) 物料主档参考价 material_archives.reference_price（分）
        Long materialId = parseLongOrNull(code.getMaterialId());
        if (materialId != null && materialArchiveMapper != null) {
            try {
                MaterialArchive archive = materialArchiveMapper.selectById(materialId);
                if (archive != null && archive.getReferencePrice() != null && archive.getReferencePrice() > 0) {
                    return archive.getReferencePrice();
                }
            } catch (Exception e) {
                log.warn("扫码确认入库-读取物料主档参考价失败，继续降级: materialId={}, error={}",
                        materialId, e.getMessage());
            }
        }
        return null;
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
