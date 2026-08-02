package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.WarehouseCreateDTO;
import com.foodtraceability.dto.WarehouseUpdateDTO;
import com.foodtraceability.entity.Warehouse;

import java.util.List;

/**
 * 仓库服务接口
 * 定义仓库管理相关的业务方法
 */
public interface WarehouseService extends IService<Warehouse> {

    /**
     * 创建仓库
     *
     * @param createDTO 创建请求DTO
     * @return 创建的仓库实体
     */
    Warehouse createWarehouse(WarehouseCreateDTO createDTO);

    /**
     * 更新仓库
     *
     * @param warehouseId 仓库ID
     * @param updateDTO 更新请求DTO
     * @return 更新后的仓库实体
     */
    Warehouse updateWarehouse(Long warehouseId, WarehouseUpdateDTO updateDTO);

    /**
     * 分页查询仓库列表
     *
     * @param page 分页对象
     * @param warehouseName 仓库名称（可选）
     * @param warehouseCode 仓库编码（可选）
     * @param warehouseType 仓库类型（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    IPage<Warehouse> getWarehousePage(Page<Warehouse> page,
                                     String warehouseName,
                                     String warehouseCode,
                                     Integer warehouseType,
                                     Integer status);

    /**
     * 获取所有启用的仓库列表
     *
     * @return 仓库列表
     */
    List<Warehouse> getActiveWarehouses();

    /**
     * 启用/停用仓库
     *
     * @param warehouseId 仓库ID
     * @param status 状态（1:启用 0:停用）
     */
    void toggleWarehouseStatus(Long warehouseId, Integer status);

    /**
     * 删除仓库（逻辑删除）
     *
     * @param warehouseId 仓库ID
     */
    void deleteWarehouse(Long warehouseId);
}
