package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.InventorySettingQueryDTO;
import com.foodtraceability.dto.InventorySettingUpdateDTO;
import com.foodtraceability.entity.InventorySetting;

import java.util.List;
import java.util.Map;

/**
 * 库存设置服务接口
 * 定义库存设置相关的业务方法
 */
public interface InventorySettingService extends IService<InventorySetting> {

    /**
     * 获取所有库存设置，以Map形式返回
     *
     * @return 设置键值对Map
     */
    Map<String, Object> getAllSettingsAsMap();

    /**
     * 根据键名获取设置值
     *
     * @param key 设置键名
     * @return 设置值，不存在返回null
     */
    String getSettingValue(String key);

    /**
     * 根据键名获取设置实体
     *
     * @param key 设置键名
     * @return 设置实体
     */
    InventorySetting getByKey(String key);

    /**
     * 查询设置列表
     *
     * @param queryDTO 查询条件
     * @return 设置列表
     */
    List<InventorySetting> listSettings(InventorySettingQueryDTO queryDTO);

    /**
     * 更新设置值
     *
     * @param updateDTO 更新请求DTO
     * @return 更新后的设置实体
     */
    InventorySetting updateSetting(InventorySettingUpdateDTO updateDTO);

    /**
     * 批量更新设置值
     *
     * @param settings 设置键值对Map
     * @return 更新后的设置Map
     */
    Map<String, Object> batchUpdateSettings(Map<String, Object> settings);
}
