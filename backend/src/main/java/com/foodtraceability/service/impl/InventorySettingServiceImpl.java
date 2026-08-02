package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.InventorySettingQueryDTO;
import com.foodtraceability.dto.InventorySettingUpdateDTO;
import com.foodtraceability.entity.InventorySetting;
import com.foodtraceability.mapper.InventorySettingMapper;
import com.foodtraceability.service.InventorySettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存设置服务实现类
 * 实现库存设置的查询、更新等业务逻辑
 */
@Service
public class InventorySettingServiceImpl extends ServiceImpl<InventorySettingMapper, InventorySetting>
        implements InventorySettingService {

    private static final Logger log = LoggerFactory.getLogger(InventorySettingServiceImpl.class);

    private final InventorySettingMapper inventorySettingMapper;

    public InventorySettingServiceImpl(InventorySettingMapper inventorySettingMapper) {
        this.inventorySettingMapper = inventorySettingMapper;
    }

    @Override
    public Map<String, Object> getAllSettingsAsMap() {
        List<InventorySetting> settings = list();
        // 保持插入顺序
        Map<String, Object> result = new LinkedHashMap<>();
        for (InventorySetting setting : settings) {
            result.put(setting.getSettingKey(), convertValue(setting));
        }
        return result;
    }

    @Override
    public String getSettingValue(String key) {
        InventorySetting setting = getByKey(key);
        return setting != null ? setting.getSettingValue() : null;
    }

    @Override
    public InventorySetting getByKey(String key) {
        LambdaQueryWrapper<InventorySetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventorySetting::getSettingKey, key);
        return inventorySettingMapper.selectOne(wrapper);
    }

    @Override
    public List<InventorySetting> listSettings(InventorySettingQueryDTO queryDTO) {
        LambdaQueryWrapper<InventorySetting> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO != null) {
            if (queryDTO.getSettingKey() != null && !queryDTO.getSettingKey().isEmpty()) {
                wrapper.like(InventorySetting::getSettingKey, queryDTO.getSettingKey());
            }
            if (queryDTO.getSettingType() != null && !queryDTO.getSettingType().isEmpty()) {
                wrapper.eq(InventorySetting::getSettingType, queryDTO.getSettingType());
            }
        }

        wrapper.orderByAsc(InventorySetting::getSettingId);
        return inventorySettingMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventorySetting updateSetting(InventorySettingUpdateDTO updateDTO) {
        InventorySetting setting = getByKey(updateDTO.getSettingKey());
        if (setting == null) {
            // 设置项不存在，创建新的
            setting = new InventorySetting();
            setting.setSettingKey(updateDTO.getSettingKey());
            setting.setSettingValue(updateDTO.getSettingValue());
            setting.setSettingType(inferType(updateDTO.getSettingValue()));
            setting.setDescription(updateDTO.getDescription());
            setting.setCreateTime(LocalDateTime.now());
            setting.setUpdateTime(LocalDateTime.now());
            inventorySettingMapper.insert(setting);
            log.info("创建库存设置项：key={}", updateDTO.getSettingKey());
        } else {
            // 更新已有设置项
            setting.setSettingValue(updateDTO.getSettingValue());
            if (updateDTO.getDescription() != null) {
                setting.setDescription(updateDTO.getDescription());
            }
            setting.setSettingType(inferType(updateDTO.getSettingValue()));
            setting.setUpdateTime(LocalDateTime.now());
            inventorySettingMapper.updateById(setting);
            log.info("更新库存设置项：key={}", updateDTO.getSettingKey());
        }
        return setting;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchUpdateSettings(Map<String, Object> settings) {
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            InventorySettingUpdateDTO updateDTO = new InventorySettingUpdateDTO();
            updateDTO.setSettingKey(entry.getKey());
            updateDTO.setSettingValue(String.valueOf(entry.getValue()));
            updateSetting(updateDTO);
        }
        return getAllSettingsAsMap();
    }

    /**
     * 根据值的实际内容推断类型
     */
    private String inferType(String value) {
        if (value == null) {
            return "string";
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return "boolean";
        }
        try {
            Long.parseLong(value);
            return "integer";
        } catch (NumberFormatException e) {
            // 不是整数
        }
        if (value.startsWith("{") || value.startsWith("[")) {
            return "json";
        }
        return "string";
    }

    /**
     * 将设置值转换为对应的Java类型
     */
    private Object convertValue(InventorySetting setting) {
        String value = setting.getSettingValue();
        String type = setting.getSettingType();

        if (value == null) {
            return null;
        }

        if ("boolean".equals(type)) {
            return Boolean.parseBoolean(value);
        }
        if ("integer".equals(type)) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return value;
            }
        }
        return value;
    }
}
