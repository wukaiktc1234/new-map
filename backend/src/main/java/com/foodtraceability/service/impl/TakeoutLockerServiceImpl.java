package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.TakeoutLocker;
import com.foodtraceability.entity.LockerSlot;
import com.foodtraceability.mapper.TakeoutLockerMapper;
import com.foodtraceability.mapper.LockerSlotMapper;
import com.foodtraceability.service.TakeoutLockerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 外卖取餐柜服务实现类
 * @author example
 * @since 2026-01-08
 */
@Service
public class TakeoutLockerServiceImpl extends ServiceImpl<TakeoutLockerMapper, TakeoutLocker> implements TakeoutLockerService {

    private static final Logger logger = LoggerFactory.getLogger(TakeoutLockerServiceImpl.class);


    public TakeoutLockerServiceImpl(TakeoutLockerMapper takeoutLockerMapper, LockerSlotMapper lockerSlotMapper) {
        this.takeoutLockerMapper = takeoutLockerMapper;
        this.lockerSlotMapper = lockerSlotMapper;
    }

    private final TakeoutLockerMapper takeoutLockerMapper;

    private final LockerSlotMapper lockerSlotMapper;

    // 模拟设备连接状态
    private Map<Long, Boolean> lockerConnectionStatus = new HashMap<>();

    @Override
    public Map<String, Object> connectLocker(Long lockerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            TakeoutLocker locker = getById(lockerId);
            if (locker == null) {
                result.put("success", false);
                result.put("message", "取餐柜不存在");
                return result;
            }

            // 模拟设备连接
            logger.info("连接取餐柜: {} - {}", locker.getLockerCode(), locker.getLockerName());
            
            // 根据连接类型执行不同的连接逻辑
            String connectionType = locker.getConnectionType();
            switch (connectionType) {
                case "TCP":
                    // 网络连接逻辑
                    logger.info("使用TCP连接: {}:{}", locker.getDeviceIp(), locker.getDevicePort());
                    break;
                case "SERIAL":
                    // 串口连接逻辑
                    logger.info("使用串口连接");
                    break;
                case "BLUETOOTH":
                    // 蓝牙连接逻辑
                    logger.info("使用蓝牙连接");
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "不支持的连接类型");
                    return result;
            }

            // 模拟连接成功
            lockerConnectionStatus.put(lockerId, true);
            locker.setStatus("ONLINE");
            updateById(locker);

            result.put("success", true);
            result.put("message", "取餐柜连接成功");
            result.put("locker", locker);
        } catch (Exception e) {
            logger.error("连接取餐柜失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "连接取餐柜失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> disconnectLocker(Long lockerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            TakeoutLocker locker = getById(lockerId);
            if (locker == null) {
                result.put("success", false);
                result.put("message", "取餐柜不存在");
                return result;
            }

            // 模拟设备断开
            logger.info("断开取餐柜连接: {} - {}", locker.getLockerCode(), locker.getLockerName());
            lockerConnectionStatus.put(lockerId, false);
            locker.setStatus("OFFLINE");
            updateById(locker);

            result.put("success", true);
            result.put("message", "取餐柜断开成功");
        } catch (Exception e) {
            logger.error("断开取餐柜连接失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "断开取餐柜连接失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getLockerStatus(Long lockerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            TakeoutLocker locker = getById(lockerId);
            if (locker == null) {
                result.put("success", false);
                result.put("message", "取餐柜不存在");
                return result;
            }

            // 检查连接状态
            boolean isConnected = lockerConnectionStatus.getOrDefault(lockerId, false);
            String status = isConnected ? "ONLINE" : "OFFLINE";

            result.put("success", true);
            result.put("status", status);
            result.put("locker", locker);
            result.put("isConnected", isConnected);
            result.put("lastOperationTime", locker.getLastOperationTime());
            result.put("todayUsageCount", locker.getTodayUsageCount());
            result.put("availableSlots", locker.getAvailableSlots());
        } catch (Exception e) {
            logger.error("获取取餐柜状态失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取取餐柜状态失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> assignSlot(Long lockerId, Long orderId, String slotType, 
                                         String putOperator, String expectedPickupTime) {
        Map<String, Object> result = new HashMap<>();
        try {
            TakeoutLocker locker = getById(lockerId);
            if (locker == null) {
                result.put("success", false);
                result.put("message", "取餐柜不存在");
                return result;
            }

            boolean isConnected = lockerConnectionStatus.getOrDefault(lockerId, false);
            if (!isConnected) {
                result.put("success", false);
                result.put("message", "取餐柜未连接");
                return result;
            }

            // 查找可用格子
            List<LockerSlot> availableSlots = lockerSlotMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LockerSlot>()
                            .eq("locker_id", lockerId)
                            .eq("status", "AVAILABLE")
                            .eq("slot_type", slotType)
                            .last("LIMIT 1")
            );

            if (availableSlots.isEmpty()) {
                result.put("success", false);
                result.put("message", "没有可用的格子");
                return result;
            }

            LockerSlot slot = availableSlots.get(0);
            // 生成取餐码
            String pickupCode = generatePickupCode();

            // 更新格子状态
            slot.setStatus("OCCUPIED");
            slot.setOrderId(orderId);
            slot.setPickupCode(pickupCode);
            slot.setExpectedPickupTime(LocalDateTime.parse(expectedPickupTime, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            slot.setUpdateTime(LocalDateTime.now());
            slot.setUpdateBy(putOperator);

            lockerSlotMapper.updateById(slot);

            // 更新取餐柜可用格子数
            locker.setAvailableSlots(locker.getAvailableSlots() - 1);
            locker.setLastOperationTime(LocalDateTime.now());
            updateById(locker);

            result.put("success", true);
            result.put("message", "格子分配成功");
            result.put("slot", slot);
            result.put("pickupCode", pickupCode);
            result.put("locker", locker);
        } catch (Exception e) {
            logger.error("分配格子失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "分配格子失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> putTakeout(Long slotId, Long orderId, String putOperator) {
        Map<String, Object> result = new HashMap<>();
        try {
            LockerSlot slot = lockerSlotMapper.selectById(slotId);
            if (slot == null) {
                result.put("success", false);
                result.put("message", "格子不存在");
                return result;
            }

            if (!"OCCUPIED".equals(slot.getStatus())) {
                result.put("success", false);
                result.put("message", "格子未分配");
                return result;
            }

            // 更新格子状态
            slot.setPutTime(LocalDateTime.now());
            slot.setPutOperator(putOperator);
            slot.setUpdateTime(LocalDateTime.now());
            slot.setUpdateBy(putOperator);

            lockerSlotMapper.updateById(slot);

            // 更新取餐柜信息
            TakeoutLocker locker = getById(slot.getLockerId());
            if (locker != null) {
                locker.setLastOperationTime(LocalDateTime.now());
                locker.setTodayUsageCount(locker.getTodayUsageCount() != null ? 
                                        locker.getTodayUsageCount() + 1 : 1);
                updateById(locker);
            }

            result.put("success", true);
            result.put("message", "外卖放入成功");
            result.put("slot", slot);
        } catch (Exception e) {
            logger.error("放入外卖失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "放入外卖失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> pickupTakeout(String pickupCode, String pickupOperator) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 查找取餐码对应的格子
            List<LockerSlot> slots = lockerSlotMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LockerSlot>()
                            .eq("pickup_code", pickupCode)
                            .eq("status", "OCCUPIED")
                            .last("LIMIT 1")
            );

            if (slots.isEmpty()) {
                result.put("success", false);
                result.put("message", "取餐码无效或已使用");
                return result;
            }

            LockerSlot slot = slots.get(0);
            // 更新格子状态
            slot.setStatus("AVAILABLE");
            slot.setPickupTime(LocalDateTime.now());
            slot.setPickupOperator(pickupOperator);
            slot.setOrderId(null);
            slot.setPickupCode(null);
            slot.setUpdateTime(LocalDateTime.now());
            slot.setUpdateBy(pickupOperator);

            lockerSlotMapper.updateById(slot);

            // 更新取餐柜可用格子数
            TakeoutLocker locker = getById(slot.getLockerId());
            if (locker != null) {
                locker.setAvailableSlots(locker.getAvailableSlots() + 1);
                locker.setLastOperationTime(LocalDateTime.now());
                updateById(locker);
            }

            result.put("success", true);
            result.put("message", "取餐成功");
            result.put("slot", slot);
        } catch (Exception e) {
            logger.error("取餐失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "取餐失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> batchGetLockerStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取所有取餐柜
            List<TakeoutLocker> lockers = list();
            List<Map<String, Object>> lockerStatusList = new ArrayList<>();

            for (TakeoutLocker locker : lockers) {
                Map<String, Object> lockerStatus = new HashMap<>();
                boolean isConnected = lockerConnectionStatus.getOrDefault(locker.getId(), false);
                String status = isConnected ? "ONLINE" : "OFFLINE";
                
                lockerStatus.put("id", locker.getId());
                lockerStatus.put("lockerCode", locker.getLockerCode());
                lockerStatus.put("lockerName", locker.getLockerName());
                lockerStatus.put("status", status);
                lockerStatus.put("isConnected", isConnected);
                lockerStatus.put("lockerType", locker.getLockerType());
                lockerStatus.put("location", locker.getLocation());
                lockerStatus.put("totalSlots", locker.getTotalSlots());
                lockerStatus.put("availableSlots", locker.getAvailableSlots());
                lockerStatus.put("lastOperationTime", locker.getLastOperationTime());
                lockerStatus.put("todayUsageCount", locker.getTodayUsageCount());
                lockerStatusList.add(lockerStatus);
            }

            result.put("success", true);
            result.put("lockers", lockerStatusList);
            result.put("total", lockerStatusList.size());
        } catch (Exception e) {
            logger.error("批量获取取餐柜状态失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "批量获取取餐柜状态失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getLockerSlotsStatus(Long lockerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            TakeoutLocker locker = getById(lockerId);
            if (locker == null) {
                result.put("success", false);
                result.put("message", "取餐柜不存在");
                return result;
            }

            // 获取所有格子
            List<LockerSlot> slots = lockerSlotMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LockerSlot>()
                            .eq("locker_id", lockerId)
            );

            result.put("success", true);
            result.put("slots", slots);
            result.put("totalSlots", slots.size());
            result.put("locker", locker);
        } catch (Exception e) {
            logger.error("获取取餐柜格子状态失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取取餐柜格子状态失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> releaseSlot(Long slotId, String operator) {
        Map<String, Object> result = new HashMap<>();
        try {
            LockerSlot slot = lockerSlotMapper.selectById(slotId);
            if (slot == null) {
                result.put("success", false);
                result.put("message", "格子不存在");
                return result;
            }

            // 更新格子状态
            slot.setStatus("AVAILABLE");
            slot.setOrderId(null);
            slot.setPickupCode(null);
            slot.setPutTime(null);
            slot.setExpectedPickupTime(null);
            slot.setPickupTime(null);
            slot.setPutOperator(null);
            slot.setPickupOperator(null);
            slot.setUpdateTime(LocalDateTime.now());
            slot.setUpdateBy(operator);

            lockerSlotMapper.updateById(slot);

            // 更新取餐柜可用格子数
            TakeoutLocker locker = getById(slot.getLockerId());
            if (locker != null) {
                locker.setAvailableSlots(locker.getAvailableSlots() + 1);
                locker.setLastOperationTime(LocalDateTime.now());
                updateById(locker);
            }

            result.put("success", true);
            result.put("message", "格子释放成功");
            result.put("slot", slot);
        } catch (Exception e) {
            logger.error("释放格子失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "释放格子失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> resetDailyUsageCount(Long lockerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            TakeoutLocker locker = getById(lockerId);
            if (locker == null) {
                result.put("success", false);
                result.put("message", "取餐柜不存在");
                return result;
            }

            // 重置今日使用计数
            locker.setTodayUsageCount(0);
            updateById(locker);

            result.put("success", true);
            result.put("message", "取餐柜今日使用计数已重置");
            result.put("locker", locker);
        } catch (Exception e) {
            logger.error("重置取餐柜计数失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "重置取餐柜计数失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> testLocker(Long lockerId) {
        Map<String, Object> result = new HashMap<>();
        try {
            TakeoutLocker locker = getById(lockerId);
            if (locker == null) {
                result.put("success", false);
                result.put("message", "取餐柜不存在");
                return result;
            }

            boolean isConnected = lockerConnectionStatus.getOrDefault(lockerId, false);
            if (!isConnected) {
                result.put("success", false);
                result.put("message", "取餐柜未连接");
                return result;
            }

            // 模拟测试
            logger.info("测试取餐柜: {}", locker.getLockerName());

            result.put("success", true);
            result.put("message", "取餐柜测试成功");
            result.put("locker", locker);
            result.put("testTime", LocalDateTime.now());
        } catch (Exception e) {
            logger.error("测试取餐柜失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试取餐柜失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 生成取餐码
     * @return 取餐码
     */
    private String generatePickupCode() {
        return "PK" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MMddHHmm")) + 
                UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
