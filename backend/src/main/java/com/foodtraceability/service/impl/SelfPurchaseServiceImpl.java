package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.entity.SelfPurchase;
import com.foodtraceability.entity.SelfPurchaseItem;
import com.foodtraceability.entity.TraceCode;
import com.foodtraceability.mapper.SelfPurchaseMapper;
import com.foodtraceability.mapper.SelfPurchaseItemMapper;
import com.foodtraceability.service.SelfPurchaseService;
import com.foodtraceability.service.TraceCodeService;
import com.foodtraceability.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 自采服务实现类
 * 实现自采管理的具体业务逻辑
 */
@Service
public class SelfPurchaseServiceImpl extends ServiceImpl<SelfPurchaseMapper, SelfPurchase> 
        implements SelfPurchaseService {
    

    public SelfPurchaseServiceImpl(SelfPurchaseMapper selfPurchaseMapper, SelfPurchaseItemMapper selfPurchaseItemMapper, TraceCodeService traceCodeService, InventoryService inventoryService) {
        this.selfPurchaseMapper = selfPurchaseMapper;
        this.selfPurchaseItemMapper = selfPurchaseItemMapper;
        this.traceCodeService = traceCodeService;
        this.inventoryService = inventoryService;
    }

    private final SelfPurchaseMapper selfPurchaseMapper;
    
    private final SelfPurchaseItemMapper selfPurchaseItemMapper;
    
    private final TraceCodeService traceCodeService;
    
    private final InventoryService inventoryService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createSelfPurchase(SelfPurchase selfPurchase, List<SelfPurchaseItem> items) {
        // 生成自采编号
        String purchaseNumber = "SP" + LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        selfPurchase.setPurchaseNumber(purchaseNumber);
        selfPurchase.setStatus("pending");
        selfPurchase.setReimburseStatus("unreimbursed");
        
        // 保存自采记录
        boolean result = this.save(selfPurchase);
        
        if (result) {
            // 保存自采商品明细
            for (SelfPurchaseItem item : items) {
                item.setSelfPurchaseId(selfPurchase.getSelfPurchaseId());
                item.setStatus("pending");
                item.setTraceCodeStatus("pending");
                selfPurchaseItemMapper.insert(item);
            }
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean inboundSelfPurchase(String selfPurchaseId) {
        // 查询自采记录
        SelfPurchase selfPurchase = this.getById(selfPurchaseId);
        if (selfPurchase == null || !"pending".equals(selfPurchase.getStatus())) {
            return false;
        }
        
        // 获取仓库信息，使用默认值
        Long warehouseId = 1L;
        String warehouseName = "主仓库";
        
        // 查询自采商品明细
        List<SelfPurchaseItem> items = selfPurchaseItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SelfPurchaseItem>()
                        .eq("self_purchase_id", selfPurchaseId));
        
        // 处理每个商品的入库
        for (SelfPurchaseItem item : items) {
            // 生成追溯码
            List<TraceCode> traceCodes = traceCodeService.generateTraceCodes(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    "self",
                    selfPurchaseId,
                    "B" + LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                    warehouseId,
                    warehouseName
            );
            
            // 更新库存
            InventoryIncreaseDTO increaseDTO = new InventoryIncreaseDTO();
            increaseDTO.setMaterialId(item.getProductId());
            increaseDTO.setWarehouseId(warehouseId);
            increaseDTO.setQuantity(BigDecimal.valueOf(item.getQuantity()));
            increaseDTO.setTransactionType(1); // 采购入库
            increaseDTO.setReferenceType("self_purchase");
            increaseDTO.setReferenceNo(selfPurchaseId);
            inventoryService.increaseInventory(increaseDTO);
            
            // 更新自采商品明细状态
            item.setStatus("completed");
            item.setTraceCodeStatus("completed");
            selfPurchaseItemMapper.updateById(item);
        }
        
        // 更新自采记录状态
        selfPurchase.setStatus("completed");
        selfPurchase.setPurchaseDate(LocalDateTime.now());
        return this.updateById(selfPurchase);
    }
    
    @Override
    public boolean reimburseSelfPurchase(String selfPurchaseId) {
        // 查询自采记录
        SelfPurchase selfPurchase = this.getById(selfPurchaseId);
        if (selfPurchase == null || !"completed".equals(selfPurchase.getStatus())) {
            return false;
        }
        
        // 更新报销状态
        selfPurchase.setReimburseStatus("reimbursed");
        return this.updateById(selfPurchase);
    }
    
    @Override
    public SelfPurchase getSelfPurchaseById(String selfPurchaseId) {
        return this.getById(selfPurchaseId);
    }
    
    @Override
    public List<SelfPurchaseItem> getSelfPurchaseItems(String selfPurchaseId) {
        return selfPurchaseItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SelfPurchaseItem>()
                        .eq("self_purchase_id", selfPurchaseId));
    }
    
    @Override
    public boolean cancelSelfPurchase(String selfPurchaseId) {
        // 查询自采记录
        SelfPurchase selfPurchase = this.getById(selfPurchaseId);
        if (selfPurchase == null || "completed".equals(selfPurchase.getStatus())) {
            return false;
        }
        
        // 更新状态为已取消
        selfPurchase.setStatus("cancelled");
        return this.updateById(selfPurchase);
    }
}
