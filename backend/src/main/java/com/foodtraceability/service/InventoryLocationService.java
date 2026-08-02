package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.InventoryLocationCreateDTO;
import com.foodtraceability.dto.InventoryLocationUpdateDTO;
import com.foodtraceability.dto.InventoryLocationVO;
import com.foodtraceability.entity.InventoryLocation;

import java.util.List;

/**
 * 库位服务接口
 * 定义库位管理相关的业务方法
 */
public interface InventoryLocationService extends IService<InventoryLocation> {

    /**
     * 创建库位
     *
     * @param createDTO 创建请求DTO
     * @return 创建的库位实体
     */
    InventoryLocation createLocation(InventoryLocationCreateDTO createDTO);

    /**
     * 更新库位
     *
     * @param locationId 库位ID
     * @param updateDTO 更新请求DTO
     * @return 更新后的库位实体
     */
    InventoryLocation updateLocation(Long locationId, InventoryLocationUpdateDTO updateDTO);

    /**
     * 分页查询库位列表（返回VO）
     *
     * @param page 分页对象
     * @param warehouseId 仓库ID（可选）
     * @param locationCode 库位编码（可选）
     * @param locationType 库位类型（可选）
     * @param status 状态（可选）
     * @return 分页结果（VO）
     */
    IPage<InventoryLocationVO> getLocationPage(Page<InventoryLocation> page,
                                                Long warehouseId,
                                                String locationCode,
                                                Integer locationType,
                                                Integer status);

    /**
     * 获取库位详情（返回VO）
     *
     * @param locationId 库位ID
     * @return 库位VO
     */
    InventoryLocationVO getLocationDetail(Long locationId);

    /**
     * 启用/停用库位
     *
     * @param locationId 库位ID
     * @param status 状态（1:启用 0:停用）
     */
    void toggleLocationStatus(Long locationId, Integer status);

    /**
     * 根据仓库ID获取库位列表
     *
     * @param warehouseId 仓库ID
     * @return 库位列表
     */
    List<InventoryLocation> getLocationsByWarehouseId(Long warehouseId);

    /**
     * 删除库位（逻辑删除）
     * 用于清理废弃库位，业务核心数据不物理删除
     *
     * @param locationId 库位ID
     */
    void deleteLocation(Long locationId);
}
