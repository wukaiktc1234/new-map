package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryTransfer;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 调拨单Mapper接口
 * 提供调拨单相关的数据库操作
 */
@Repository
public interface InventoryTransferMapper extends BaseMapper<InventoryTransfer> {

    /**
     * 分页查询调拨单列表
     *
     * @param page 分页对象
     * @param transferNo 调拨单号
     * @param fromWarehouseId 源仓库ID
     * @param toWarehouseId 目标仓库ID
     * @param status 状态
     * @param applyTimeStart 申请时间开始
     * @param applyTimeEnd 申请时间结束
     * @return 分页结果
     */
    IPage<InventoryTransfer> selectInventoryTransferPage(Page<InventoryTransfer> page,
                                                         @Param("transferNo") String transferNo,
                                                         @Param("fromWarehouseId") Long fromWarehouseId,
                                                         @Param("toWarehouseId") Long toWarehouseId,
                                                         @Param("status") String status,
                                                         @Param("applyTimeStart") String applyTimeStart,
                                                         @Param("applyTimeEnd") String applyTimeEnd);
}
