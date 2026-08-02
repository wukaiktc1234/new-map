package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Inventory;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 库存Mapper接口
 * 提供库存相关的数据库操作
 */
@Repository
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 分页查询库存列表
     *
     * @param page 分页对象
     * @param warehouseId 仓库ID（可选）
     * @param materialId 物料ID（可选）
     * @param materialName 物料名称（可选，模糊查询）
     * @param batchNo 批次号（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    IPage<Inventory> selectInventoryPage(Page<Inventory> page,
                                        @Param("warehouseId") Long warehouseId,
                                        @Param("materialId") Long materialId,
                                        @Param("materialName") String materialName,
                                        @Param("batchNo") String batchNo,
                                        @Param("status") Integer status);

    /**
     * 根据物料ID和仓库ID查询库存
     *
     * @param materialId 物料ID
     * @param warehouseId 仓库ID
     * @return 库存实体
     */
    Inventory selectByMaterialAndWarehouse(@Param("materialId") Long materialId,
                                          @Param("warehouseId") Long warehouseId);
}
