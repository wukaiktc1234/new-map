package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryLog;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 库存日志Mapper接口
 * 提供库存日志相关的数据库操作
 */
@Repository
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {
    
    /**
     * 分页查询库存日志列表（包含条件查询）
     *
     * @param page 分页对象
     * @param productId 产品ID（可选）
     * @param warehouseId 仓库ID（可选）
     * @param operationType 操作类型（可选）
     * @param operatorId 操作人ID（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 分页结果
     */
    IPage<InventoryLog> selectInventoryLogPage(Page<InventoryLog> page,
                                              @Param("productId") Long productId,
                                              @Param("warehouseId") Long warehouseId,
                                              @Param("operationType") String operationType,
                                              @Param("operatorId") Long operatorId,
                                              @Param("startTime") String startTime,
                                              @Param("endTime") String endTime);
    
    /**
     * 查询库存消耗统计数据
     *
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param operationType 操作类型（可选）
     * @return 统计数据
     */
    java.util.Map<String, Object> selectConsumptionStats(@Param("startTime") String startTime,
                                                       @Param("endTime") String endTime,
                                                       @Param("operationType") String operationType);
}