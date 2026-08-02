package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryAdjust;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 库存调整单Mapper接口
 * 提供库存调整单相关的数据库操作
 */
@Repository
public interface InventoryAdjustMapper extends BaseMapper<InventoryAdjust> {

    /**
     * 分页查询库存调整单列表
     *
     * @param page 分页对象
     * @param adjustType 调整类型
     * @param status 状态
     * @param warehouseId 仓库ID
     * @param keyword 关键词（调整单号/物料名称）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分页结果
     */
    IPage<InventoryAdjust> selectAdjustPage(Page<InventoryAdjust> page,
                                             @Param("adjustType") String adjustType,
                                             @Param("status") String status,
                                             @Param("warehouseId") String warehouseId,
                                             @Param("keyword") String keyword,
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate);
}
