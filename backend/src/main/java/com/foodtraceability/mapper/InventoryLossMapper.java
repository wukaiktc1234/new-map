package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryLoss;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 报损单Mapper接口
 * 提供报损单相关的数据库操作
 */
@Repository
public interface InventoryLossMapper extends BaseMapper<InventoryLoss> {

    /**
     * 分页查询报损单列表
     *
     * @param page 分页对象
     * @param lossNo 报损单号
     * @param warehouseId 仓库ID
     * @param status 状态
     * @param applyTimeStart 申请时间开始
     * @param applyTimeEnd 申请时间结束
     * @return 分页结果
     */
    IPage<InventoryLoss> selectInventoryLossPage(Page<InventoryLoss> page,
                                                  @Param("lossNo") String lossNo,
                                                  @Param("warehouseId") Long warehouseId,
                                                  @Param("status") String status,
                                                  @Param("applyTimeStart") String applyTimeStart,
                                                  @Param("applyTimeEnd") String applyTimeEnd);
}
