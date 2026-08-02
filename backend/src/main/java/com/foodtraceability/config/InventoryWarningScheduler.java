package com.foodtraceability.config;

import com.foodtraceability.entity.Inventory;
import com.foodtraceability.entity.InventoryWarningRecord;
import com.foodtraceability.entity.InventoryWarningRule;
import com.foodtraceability.mapper.InventoryMapper;
import com.foodtraceability.mapper.InventoryWarningRecordMapper;
import com.foodtraceability.mapper.InventoryWarningRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存预警定时任务
 * 定时检查库存预警规则，生成预警记录
 */
@Component
public class InventoryWarningScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InventoryWarningScheduler.class);

    private final InventoryWarningRuleMapper warningRuleMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryWarningRecordMapper warningRecordMapper;

    public InventoryWarningScheduler(InventoryWarningRuleMapper warningRuleMapper,
                                    InventoryMapper inventoryMapper,
                                    InventoryWarningRecordMapper warningRecordMapper) {
        this.warningRuleMapper = warningRuleMapper;
        this.inventoryMapper = inventoryMapper;
        this.warningRecordMapper = warningRecordMapper;
    }

    /**
     * 每小时执行一次库存预警检查
     * cron表达式：每小时第0分钟执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkInventoryWarnings() {
        logger.info("开始执行库存预警检查...");

        try {
            // 1. 获取所有启用的预警规则
            List<InventoryWarningRule> rules = warningRuleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InventoryWarningRule>()
                            .eq(InventoryWarningRule::getIsEnabled, true)
            );

            for (InventoryWarningRule rule : rules) {
                // 根据规则类型执行不同的检查逻辑
                switch (rule.getWarningType()) {
                    case 1:
                        checkLowStockWarning(rule);   // 低库存预警
                        break;
                    case 2:
                        checkHighStockWarning(rule);  // 高库存预警
                        break;
                    case 3:
                        checkExpiringWarning(rule);   // 临期预警
                        break;
                    case 4:
                        checkExpiredWarning(rule);    // 过期预警
                        break;
                    default:
                        logger.warn("未知的预警类型：{}", rule.getWarningType());
                }
            }

            logger.info("库存预警检查完成");
        } catch (Exception e) {
            logger.error("库存预警检查失败", e);
        }
    }

    /**
     * 低库存预警检查
     */
    private void checkLowStockWarning(InventoryWarningRule rule) {
        List<Inventory> lowStockItems = inventoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inventory>()
                        .le(Inventory::getQuantity, rule.getThresholdValue())
                        .eq(Inventory::getStatus, 1)
        );

        for (Inventory inventory : lowStockItems) {
            createWarningRecord(rule, inventory,
                    "低库存预警：" + inventory.getMaterialName() +
                    " 当前库存 " + inventory.getQuantity() +
                    "，阈值 " + rule.getThresholdValue());
        }
    }

    /**
     * 高库存预警检查
     */
    private void checkHighStockWarning(InventoryWarningRule rule) {
        List<Inventory> highStockItems = inventoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inventory>()
                        .ge(Inventory::getQuantity, rule.getThresholdValue())
                        .isNotNull(Inventory::getMaxStockQty)
                        .eq(Inventory::getStatus, 1)
        );

        for (Inventory inventory : highStockItems) {
            if (inventory.getMaxStockQty() != null &&
                    inventory.getQuantity().compareTo(inventory.getMaxStockQty()) > 0) {
                createWarningRecord(rule, inventory,
                        "高库存预警：" + inventory.getMaterialName() +
                        " 当前库存 " + inventory.getQuantity() +
                        "，最大库存 " + inventory.getMaxStockQty());
            }
        }
    }

    /**
     * 临期预警检查
     */
    private void checkExpiringWarning(InventoryWarningRule rule) {
        LocalDateTime expiryThreshold = LocalDateTime.now().plusDays(
                rule.getThresholdValue().intValue()
        );

        List<Inventory> expiringItems = inventoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inventory>()
                        .isNotNull(Inventory::getExpiryDate)
                        .le(Inventory::getExpiryDate, java.time.LocalDate.now().plusDays(rule.getThresholdValue().intValue()))
                        .ge(Inventory::getExpiryDate, java.time.LocalDate.now())
                        .ne(Inventory::getStatus, 3)
                        .ne(Inventory::getStatus, 4)
        );

        for (Inventory inventory : expiringItems) {
            createWarningRecord(rule, inventory,
                    "临期预警：" + inventory.getMaterialName() +
                    " 有效期至 " + inventory.getExpiryDate());
        }
    }

    /**
     * 过期预警检查
     */
    private void checkExpiredWarning(InventoryWarningRule rule) {
        List<Inventory> expiredItems = inventoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Inventory>()
                        .isNotNull(Inventory::getExpiryDate)
                        .lt(Inventory::getExpiryDate, java.time.LocalDate.now())
                        .ne(Inventory::getStatus, 3)
                        .ne(Inventory::getStatus, 4)
        );

        for (Inventory inventory : expiredItems) {
            createWarningRecord(rule, inventory,
                    "过期预警：" + inventory.getMaterialName() +
                    " 已过期，有效期至 " + inventory.getExpiryDate());

            // 自动更新库存状态为过期
            inventory.setStatus(3);
            inventory.setUpdateTime(LocalDateTime.now());
            inventoryMapper.updateById(inventory);
        }
    }

    /**
     * 创建预警记录
     */
    private void createWarningRecord(InventoryWarningRule rule, Inventory inventory, String message) {
        // 检查是否已存在未处理的相同预警
        Long existingCount = warningRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InventoryWarningRecord>()
                        .eq(InventoryWarningRecord::getRuleId, rule.getRuleId())
                        .eq(InventoryWarningRecord::getInventoryId, inventory.getInventoryId())
                        .eq(InventoryWarningRecord::getIsHandled, false)
        );

        if (existingCount > 0) {
            return; // 已存在未处理的预警，不重复创建
        }

        // 创建新的预警记录
        InventoryWarningRecord record = new InventoryWarningRecord();
        record.setRuleId(rule.getRuleId());
        record.setInventoryId(inventory.getInventoryId());
        record.setWarehouseId(inventory.getWarehouseId());
        record.setWarningType(rule.getWarningType());
        record.setCurrentValue(inventory.getQuantity());
        record.setThresholdValue(rule.getThresholdValue());
        record.setMessage(message);
        record.setIsHandled(false);
        record.setCreateTime(LocalDateTime.now());

        warningRecordMapper.insert(record);

        logger.info("生成预警记录：{}", message);
    }
}
