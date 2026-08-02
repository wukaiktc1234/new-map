package com.foodtraceability.service.impl;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.InventoryCode;
import com.foodtraceability.entity.TestResult;
import com.foodtraceability.mapper.InventoryCodeMapper;
import com.foodtraceability.service.DeviceConnectionService;
import com.foodtraceability.service.DevicePrintService;
import com.foodtraceability.service.DeviceStatusService;
import com.foodtraceability.service.HardwareDeviceService;
import com.foodtraceability.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class HardwareDeviceServiceImpl implements HardwareDeviceService {
    
    private static final Logger log = LoggerFactory.getLogger(HardwareDeviceServiceImpl.class);
    

    public HardwareDeviceServiceImpl(DeviceStatusService deviceStatusService, @Lazy DevicePrintService devicePrintService, DeviceConnectionService deviceConnectionService, InventoryService inventoryService, InventoryCodeMapper inventoryCodeMapper) {
        this.deviceStatusService = deviceStatusService;
        this.devicePrintService = devicePrintService;
        this.deviceConnectionService = deviceConnectionService;
        this.inventoryService = inventoryService;
        this.inventoryCodeMapper = inventoryCodeMapper;
    }

    private final DeviceStatusService deviceStatusService;
    
    private final DevicePrintService devicePrintService;
    
    private final DeviceConnectionService deviceConnectionService;
    
    private final InventoryService inventoryService;
    
    private final InventoryCodeMapper inventoryCodeMapper;
    
    @Override
    public Boolean scanInventoryCode(String inventoryCode) {
        try {
            log.info("扫码枪扫描库存编码: {}", inventoryCode);
            
            // 1. 根据库存编码查找对应库存记录
            InventoryCode code = inventoryCodeMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InventoryCode>()
                .eq(InventoryCode::getUniqueCode, inventoryCode));
            
            if (code == null) {
                log.warn("未找到对应库存记录: {}", inventoryCode);
                return false;
            }
            
            // 2. 获取库存详情
            Long inventoryId = code.getInventoryId();
            /* 修复：使用IService的getById方法替代自定义的getInventoryById */
            com.foodtraceability.entity.Inventory inventory = inventoryService.getById(inventoryId);

            if (inventory == null) {
                log.warn("未找到库存详情: 库存ID={}, 编码={}", inventoryId, inventoryCode);
                return false;
            }

            // 3. 执行库存操作：这里默认实现为减少库存数量1个
            // 在实际业务中，可以根据需求调整为不同的库存操作（如入库、出库、盘点等）
            /* 修复：使用quantity字段（BigDecimal）替代currentStock（int） */
            int currentStock = inventory.getQuantity() != null ? inventory.getQuantity().intValue() : 0;
            if (currentStock <= 0) {
                /* 修复：使用materialName字段替代productName */
                log.warn("库存不足: 物料={}, 当前库存={}", inventory.getMaterialName(), currentStock);
                return false;
            }

            // 4. 更新库存数量（减少1个）
            /* 修复：使用setQuantity替代setCurrentStock */
            inventory.setQuantity(BigDecimal.valueOf(currentStock - 1));
            /* 修复：使用IService的updateById方法替代自定义的updateInventory */
            inventoryService.updateById(inventory);

            // 5. 更新库存编码状态为已使用
            code.setStatus(1); // 1表示已使用
            inventoryCodeMapper.updateById(code);

            log.info("扫码成功并更新库存: 编码={}, 物料={}, 原库存={}, 现库存={}",
                inventoryCode, inventory.getMaterialName(), currentStock, inventory.getQuantity().intValue());
            return true;
        } catch (Exception e) {
            log.error("扫码失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Boolean printTraceabilityLabel(String traceabilityCode, String productName) {
        try {
            return devicePrintService.printTraceabilityLabel(traceabilityCode, productName);
        } catch (Exception e) {
            log.error("打印异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Boolean captureMealPhoto(String orderId, String traceabilityCode) {
        try {
            log.info("拍摄出餐照片: 订单ID={}, 追溯码={}", orderId, traceabilityCode);
            
            HardwareConfig cameraConfig = deviceStatusService.getHardwareConfig("CAMERA");
            if (cameraConfig == null) {
                log.warn("摄像头配置不存在");
                return false;
            }
            
            // 简化摄像头逻辑，实际项目中应调用具体的摄像头服务
            log.info("照片拍摄功能未实现");
            return false;
        } catch (Exception e) {
            log.error("拍摄照片异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Boolean testConnection(HardwareConfig config) {
        try {
            return deviceConnectionService.testConnection(config);
        } catch (Exception e) {
            log.error("测试连接失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public TestResult testConnectionDetailed(HardwareConfig config) {
        try {
            return deviceConnectionService.testConnectionDetailed(config);
        } catch (Exception e) {
            log.error("详细测试连接失败: {}", e.getMessage(), e);
            // 构造失败的测试结果
            return TestResult.failure(config.getDeviceType(), config.getDeviceName(), 
                config.getConnectionType() != null ? config.getConnectionType() : "UNKNOWN",
                0L, "测试失败", e.getMessage(), "testConnectionDetailed");
        }
    }
    
    @Override
    public Boolean getDeviceStatus(String deviceType) {
        try {
            return deviceStatusService.getDeviceStatus(deviceType);
        } catch (Exception e) {
            log.error("获取设备状态失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public java.util.List<HardwareConfig> getAllDevices(Long storeId) {
        try {
            return deviceStatusService.getAllDevices(storeId);
        } catch (Exception e) {
            log.error("获取所有设备失败: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
}