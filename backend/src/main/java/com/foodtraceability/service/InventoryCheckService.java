package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.InventoryCheckCreateDTO;
import com.foodtraceability.entity.InventoryCheck;

import java.util.List;
import java.util.Map;

/**
 * 盘点单服务接口
 */
public interface InventoryCheckService extends IService<InventoryCheck> {

    /**
     * 创建盘点单
     */
    InventoryCheck createCheck(InventoryCheckCreateDTO createDTO);

    /**
     * 获取仓库的盘点列表
     */
    List<InventoryCheck> getChecksByWarehouse(Long warehouseId);

    /**
     * 更新盘点单（仅草稿状态可更新）
     *
     * @param checkId 盘点单ID
     * @param updateDTO 更新请求DTO
     * @return 更新后的盘点单
     */
    InventoryCheck updateCheck(Long checkId, InventoryCheckCreateDTO updateDTO);

    /**
     * 删除盘点单（仅草稿状态可删除，逻辑删除）
     *
     * @param checkId 盘点单ID
     */
    void deleteCheck(Long checkId);

    /**
     * 保存盘点条目并更新盘点单状态为盘点中
     * 将前端提交的盘点条目持久化到 inventory_check_item 表
     *
     * @param checkId 盘点单ID
     * @param items 盘点条目列表
     * @return 更新后的盘点单
     */
    InventoryCheck saveCheckItems(Long checkId, List<Map<String, Object>> items);
}
