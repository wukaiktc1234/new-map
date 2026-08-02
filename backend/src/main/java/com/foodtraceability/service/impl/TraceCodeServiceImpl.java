package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.InventoryDecreaseDTO;
import com.foodtraceability.dto.InventoryIncreaseDTO;
import com.foodtraceability.entity.TraceCode;
import com.foodtraceability.entity.TraceCodeLog;
import com.foodtraceability.mapper.TraceCodeMapper;
import com.foodtraceability.mapper.TraceCodeLogMapper;
import com.foodtraceability.service.TraceCodeService;
import com.foodtraceability.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 追溯码服务实现类
 * 实现追溯码管理的具体业务逻辑
 */
@Service
public class TraceCodeServiceImpl extends ServiceImpl<TraceCodeMapper, TraceCode> 
        implements TraceCodeService {
    

    public TraceCodeServiceImpl(TraceCodeMapper traceCodeMapper, TraceCodeLogMapper traceCodeLogMapper, InventoryService inventoryService) {
        this.traceCodeMapper = traceCodeMapper;
        this.traceCodeLogMapper = traceCodeLogMapper;
        this.inventoryService = inventoryService;
    }

    private final TraceCodeMapper traceCodeMapper;
    
    private final TraceCodeLogMapper traceCodeLogMapper;
    
    private final InventoryService inventoryService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TraceCode> generateTraceCodes(Long productId, String productName, Integer quantity, 
                                             String sourceType, String sourceId, String batchNumber,
                                             Long warehouseId, String warehouseName) {
        List<TraceCode> traceCodes = new ArrayList<>();
        List<TraceCodeLog> traceCodeLogs = new ArrayList<>();
        
        String datePrefix = LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 批量创建追溯码
        for (int i = 0; i < quantity; i++) {
            // 生成唯一追溯码
            String code = "TC" + datePrefix + 
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            TraceCode traceCode = new TraceCode();
            traceCode.setCode(code);
            traceCode.setProductId(productId);
            traceCode.setProductName(productName);
            traceCode.setBatchNumber(batchNumber);
            traceCode.setSourceType(sourceType);
            traceCode.setSourceId(sourceId);
            traceCode.setStatus(1); // created状态对应整数1
            traceCode.setWarehouseId(warehouseId);
            traceCode.setWarehouseName(warehouseName);
            traceCode.setCreateTime(LocalDateTime.now());
            traceCode.setUpdateTime(LocalDateTime.now());
            traceCode.setCreateBy("system");
            traceCode.setUpdateBy("system");
            
            traceCodes.add(traceCode);
        }
        
        // 批量保存追溯码
        if (!traceCodes.isEmpty()) {
            this.saveBatch(traceCodes, 100); // 每批保存100条
            
            // 批量生成操作日志
            for (TraceCode traceCode : traceCodes) {
                TraceCodeLog log = new TraceCodeLog();
                log.setTraceCodeId(String.valueOf(traceCode.getTraceCodeId())); // Long→String转换
                log.setCode(traceCode.getCode());
                log.setOperationType("create");
                log.setBeforeStatus(null);
                log.setAfterStatus("created");
                log.setOperator("system");
                log.setOperatorId(0L);
                log.setOperationTime(LocalDateTime.now());
                log.setOperationLocation("系统");
                log.setOperationDescription("系统自动生成");
                log.setCreateTime(LocalDateTime.now());
                
                traceCodeLogs.add(log);
            }
            
            // 批量保存操作日志
            if (!traceCodeLogs.isEmpty()) {
                // 分批保存，每批100条
                for (int i = 0; i < traceCodeLogs.size(); i += 100) {
                    int end = Math.min(i + 100, traceCodeLogs.size());
                    List<TraceCodeLog> batchLogs = traceCodeLogs.subList(i, end);
                    for (TraceCodeLog log : batchLogs) {
                        traceCodeLogMapper.insert(log);
                    }
                }
            }
        }
        
        return traceCodes;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TraceCode scanTraceCodeOutbound(String code, String operator, Long operatorId, 
                                        String location, String purpose) {
        // 查询追溯码
        TraceCode traceCode = this.getTraceCodeByCode(code);
        if (traceCode == null || !"CREATED".equals(traceCode.getStatus()) && !"INBOUND".equals(traceCode.getStatus())) {
            return null;
        }
        
        // 更新追溯码状态
        String beforeStatus = String.valueOf(traceCode.getStatus()); // Integer→String转换
        traceCode.setStatus(2); // OUTBOUND状态对应整数2
        traceCode.setOutboundTime(LocalDateTime.now());
        traceCode.setUsedBy(operator);
        traceCode.setUsagePurpose(purpose);
        traceCode.setUpdateTime(LocalDateTime.now());
        traceCode.setUpdateBy(operator);
        
        boolean result = this.updateById(traceCode);
        
        if (result) {
            // 生成操作日志
            generateTraceCodeLog(String.valueOf(traceCode.getTraceCodeId()), code, "outbound",  // Long→String转换
                               beforeStatus, "OUTBOUND", operator, operatorId, 
                               location, purpose);
            
            // 自动扣减库存
            InventoryDecreaseDTO decreaseDTO = new InventoryDecreaseDTO();
            decreaseDTO.setMaterialId(traceCode.getProductId());
            decreaseDTO.setWarehouseId(traceCode.getWarehouseId());
            decreaseDTO.setQuantity(BigDecimal.ONE);
            decreaseDTO.setTransactionType(1); // 销售出库
            decreaseDTO.setReferenceType("trace_code");
            decreaseDTO.setReferenceNo(code);
            inventoryService.decreaseInventory(decreaseDTO);
            return traceCode;
        }
        
        return null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean returnUnopenedTraceCode(String code, String operator, Long operatorId, 
                                          String location, String reason) {
        // 查询追溯码
        TraceCode traceCode = this.getTraceCodeByCode(code);
        if (traceCode == null || !"used".equals(traceCode.getStatus())) {
            return false;
        }
        
        // 更新追溯码状态
        String beforeStatus = String.valueOf(traceCode.getStatus()); // Integer→String转换
        traceCode.setStatus(4); // returned状态对应整数4
        traceCode.setUpdateTime(LocalDateTime.now());
        traceCode.setUpdateBy(operator);
        
        boolean result = this.updateById(traceCode);
        
        if (result) {
            // 生成操作日志
            generateTraceCodeLog(String.valueOf(traceCode.getTraceCodeId()), code, "return",  // Long→String转换
                               beforeStatus, "returned", operator, operatorId, 
                               location, reason);
            
            // 恢复库存
            InventoryIncreaseDTO increaseDTO = new InventoryIncreaseDTO();
            increaseDTO.setMaterialId(traceCode.getProductId());
            increaseDTO.setWarehouseId(traceCode.getWarehouseId());
            increaseDTO.setQuantity(BigDecimal.ONE);
            increaseDTO.setTransactionType(4); // 退货入库
            increaseDTO.setReferenceType("trace_code");
            increaseDTO.setReferenceNo(code);
            inventoryService.increaseInventory(increaseDTO);
        }
        
        return result;
    }
    
    @Override
    public TraceCode getTraceCodeByCode(String code) {
        // 使用lambda查询，性能更好
        return this.lambdaQuery()
                .eq(TraceCode::getCode, code)
                .one();
    }
    
    @Override
    public List<TraceCodeLog> getTraceCodeLogs(String traceCodeId) {
        return traceCodeLogMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TraceCodeLog>()
                        .eq("trace_code_id", traceCodeId)
                        .orderByDesc("operation_time"));
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean inboundTraceCode(String traceCodeId) {
        // 查询追溯码
        TraceCode traceCode = this.getById(traceCodeId);
        if (traceCode == null || !"created".equals(traceCode.getStatus())) {
            return false;
        }
        
        // 更新追溯码状态
        String beforeStatus = String.valueOf(traceCode.getStatus()); // Integer→String转换
        traceCode.setStatus(3); // inbound状态对应整数3（已入库）
        traceCode.setInboundTime(LocalDateTime.now());
        traceCode.setUpdateTime(LocalDateTime.now());
        
        boolean result = this.updateById(traceCode);
        
        if (result) {
            // 生成操作日志
            generateTraceCodeLog(traceCodeId, traceCode.getCode(), "inbound", 
                               beforeStatus, "inbound", "system", 0L, 
                               "系统自动入库", "系统");
        }
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchInboundTraceCodes(List<String> traceCodeIds) {
        for (String traceCodeId : traceCodeIds) {
            if (!inboundTraceCode(traceCodeId)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public List<TraceCode> getTraceCodesByProduct(Long productId, String status) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TraceCode> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TraceCode>()
                        .eq("product_id", productId);
        
        if (status != null) {
            wrapper.eq("status", status);
        }
        
        return this.list(wrapper);
    }
    
    /**
     * 生成追溯码操作日志
     */
    private void generateTraceCodeLog(String traceCodeId, String code, String operationType, 
                                     String beforeStatus, String afterStatus, String operator, 
                                     Long operatorId, String location, String description) {
        TraceCodeLog log = new TraceCodeLog();
        log.setTraceCodeId(traceCodeId);
        log.setCode(code);
        log.setOperationType(operationType);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setOperator(operator);
        log.setOperatorId(operatorId);
        log.setOperationTime(LocalDateTime.now());
        log.setOperationLocation(location);
        log.setOperationDescription(description);
        log.setCreateTime(LocalDateTime.now());
        
        traceCodeLogMapper.insert(log);
    }
}
