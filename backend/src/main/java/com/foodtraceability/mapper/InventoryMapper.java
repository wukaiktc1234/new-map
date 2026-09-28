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
     * 分页查询库存列表（M3-M4：统一账按 location_id 维度）
     *
     * @param page 分页对象
     * @param locationId 位置ID（可选；STORE/CENTRAL/DEPOT 统一维度）
     * @param materialId 物料ID（可选）
     * @param materialName 物料名称（可选，模糊查询）
     * @param batchNo 批次号（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    IPage<Inventory> selectInventoryPage(Page<Inventory> page,
                                        @Param("locationId") Long locationId,
                                        @Param("materialId") Long materialId,
                                        @Param("materialName") String materialName,
                                        @Param("batchNo") String batchNo,
                                        @Param("status") Integer status);

    /**
     * 根据位置ID和物料ID查询库存（统一账唯一键定位点）
     *
     * @param locationId 位置ID
     * @param materialId 物料ID
     * @return 库存实体
     */
    Inventory selectByMaterialAndLocation(@Param("locationId") Long locationId,
                                          @Param("materialId") Long materialId);
}
