package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.MaterialScanConsumeDTO;
import com.foodtraceability.entity.MaterialConsumption;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.mapper.MaterialConsumptionMapper;
import com.foodtraceability.service.MaterialConsumptionService;
import com.foodtraceability.service.MaterialTraceCodeService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 原料消耗服务实现类
 */
@Service
public class MaterialConsumptionServiceImpl extends ServiceImpl<MaterialConsumptionMapper, MaterialConsumption> implements MaterialConsumptionService {

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * 使用 ObjectProvider 替代 @Lazy，避免 Spring 6 虚拟线程环境下代理创建时序问题
     * @param materialConsumptionMapper 原料消耗Mapper
     * @param materialTraceCodeServiceProvider 原料溯源码服务提供者
     */
    public MaterialConsumptionServiceImpl(MaterialConsumptionMapper materialConsumptionMapper, ObjectProvider<MaterialTraceCodeService> materialTraceCodeServiceProvider) {
        this.materialConsumptionMapper = materialConsumptionMapper;
        this.materialTraceCodeServiceProvider = materialTraceCodeServiceProvider;
    }

    private final MaterialConsumptionMapper materialConsumptionMapper;

    private final ObjectProvider<MaterialTraceCodeService> materialTraceCodeServiceProvider;

    private static final String PREFIX = "MC";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional
    public MaterialConsumption record(MaterialScanConsumeDTO dto) {
        MaterialTraceCode traceCode = materialTraceCodeServiceProvider.getObject().getByTraceCode(dto.getMaterialTraceCode());
        if (traceCode == null) {
            return null;
        }
        
        MaterialConsumption consumption = new MaterialConsumption();
        
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        String consumptionId = PREFIX + dateStr + String.format("%04d", getNextSequence());
        
        consumption.setConsumptionId(consumptionId);
        consumption.setKitchenOrderId(dto.getKitchenOrderId());
        consumption.setOrderId(dto.getOrderId());
        consumption.setMaterialTraceCodeId(traceCode.getTraceCodeId());
        consumption.setMaterialTraceCode(dto.getMaterialTraceCode());
        try {
            consumption.setProductId(traceCode.getMaterialId() != null ? Long.parseLong(traceCode.getMaterialId()) : null);
        } catch (NumberFormatException e) {
            consumption.setProductId(null);
        }
        consumption.setProductName(traceCode.getMaterialName());
        consumption.setProductCode(traceCode.getBatchNumber());
        
        BigDecimal consumeQuantity = dto.getConsumeQuantity() != null ? 
                dto.getConsumeQuantity() : traceCode.getQuantity();
        consumption.setConsumeQuantity(consumeQuantity);
        consumption.setUnit(traceCode.getUnit());
        consumption.setUnitCost(BigDecimal.ZERO);
        
        consumption.setTotalCost(BigDecimal.ZERO);
        
        consumption.setDishId(dto.getDishId());
        consumption.setDishName(dto.getDishName());
        consumption.setConsumeTime(LocalDateTime.now());
        consumption.setOperatorId(dto.getOperatorId());
        consumption.setOperatorName(dto.getOperatorName());
        consumption.setScanDevice(dto.getScanDevice());
        consumption.setStoreId(dto.getStoreId() != null ? dto.getStoreId() : null);
        consumption.setStoreName(dto.getStoreName() != null ? dto.getStoreName() : traceCode.getStorageLocation());
        consumption.setInventoryDeducted(0);
        consumption.setRemark(dto.getRemark());
        consumption.setCreateTime(LocalDateTime.now());
        consumption.setUpdateTime(LocalDateTime.now());
        
        save(consumption);
        
        materialTraceCodeServiceProvider.getObject().useTraceCode(
                dto.getMaterialTraceCode(), 
                consumptionId, 
                dto.getOperatorId(), 
                dto.getOperatorName()
        );
        
        return consumption;
    }

    @Override
    public List<MaterialConsumption> getByKitchenOrderId(String kitchenOrderId) {
        return materialConsumptionMapper.selectByKitchenOrderId(kitchenOrderId);
    }

    @Override
    public List<MaterialConsumption> getByOrderId(String orderId) {
        return materialConsumptionMapper.selectByOrderId(orderId);
    }

    @Override
    public BigDecimal calculateOrderCost(String orderId) {
        return materialConsumptionMapper.sumCostByOrderId(orderId);
    }

    @Override
    public BigDecimal calculateStoreCost(Long storeId, LocalDateTime startTime, LocalDateTime endTime) {
        return materialConsumptionMapper.sumCostByStoreAndTime(storeId, startTime, endTime);
    }

    @Override
    @Transactional
    public boolean deductInventory(String consumptionId) {
        MaterialConsumption consumption = getById(consumptionId);
        if (consumption == null || consumption.getInventoryDeducted() == 1) {
            return false;
        }
        
        consumption.setInventoryDeducted(1);
        consumption.setInventoryDeductTime(LocalDateTime.now());
        consumption.setUpdateTime(LocalDateTime.now());
        
        return updateById(consumption);
    }

    @Override
    @Transactional
    public boolean batchDeductInventory(List<String> consumptionIds) {
        for (String consumptionId : consumptionIds) {
            deductInventory(consumptionId);
        }
        return true;
    }

    @Override
    public List<MaterialConsumption> getUndeductedRecords() {
        return materialConsumptionMapper.selectUndeducted();
    }

    private int getNextSequence() {
        List<MaterialConsumption> records = lambdaQuery()
                .likeRight(MaterialConsumption::getConsumptionId, PREFIX + LocalDate.now().format(DATE_FORMAT))
                .orderByDesc(MaterialConsumption::getConsumptionId)
                .last("LIMIT 1")
                .list();
        
        if (records.isEmpty()) {
            return 1;
        }
        String lastId = records.get(0).getConsumptionId();
        String seqStr = lastId.substring(lastId.length() - 4);
        return Integer.parseInt(seqStr) + 1;
    }
}
