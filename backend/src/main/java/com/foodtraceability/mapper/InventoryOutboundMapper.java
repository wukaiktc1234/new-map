package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryOutbound;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 库存出库单Mapper接口
 * 提供库存出库单相关的数据库操作
 */
@Repository
public interface InventoryOutboundMapper extends BaseMapper<InventoryOutbound> {

    /**
     * 分页查询库存出库单列表
     *
     * @param page 分页对象
     * @param outboundType 出库类型
     * @param status 状态
     * @param warehouseId 仓库ID
     * @param keyword 关键词（出库单号/物料名称）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分页结果
     */
    IPage<InventoryOutbound> selectOutboundPage(Page<InventoryOutbound> page,
                                                 @Param("outboundType") String outboundType,
                                                 @Param("status") String status,
                                                 @Param("warehouseId") String warehouseId,
                                                 @Param("keyword") String keyword,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);
}
