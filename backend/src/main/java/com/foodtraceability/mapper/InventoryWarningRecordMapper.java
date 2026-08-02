package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryWarningRecord;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 预警记录Mapper接口
 * 提供预警记录相关的数据库操作
 */
@Repository
public interface InventoryWarningRecordMapper extends BaseMapper<InventoryWarningRecord> {

    /**
     * 分页查询预警记录
     *
     * @param page 分页对象
     * @param warehouseId 仓库ID（可选）
     * @param warningType 预警类型（可选）
     * @param isHandled 是否已处理（可选）
     * @return 分页结果
     */
    IPage<InventoryWarningRecord> selectWarningRecordPage(Page<InventoryWarningRecord> page,
                                                          @Param("warehouseId") Long warehouseId,
                                                          @Param("warningType") Integer warningType,
                                                          @Param("isHandled") Boolean isHandled);
}
