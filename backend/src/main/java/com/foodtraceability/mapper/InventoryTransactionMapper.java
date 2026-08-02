package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryTransaction;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 库存变动记录Mapper接口
 * 提供库存变动记录的数据库操作
 */
@Repository
public interface InventoryTransactionMapper extends BaseMapper<InventoryTransaction> {

    /**
     * 分页查询库存变动记录
     *
     * @param page 分页对象
     * @param inventoryId 库存ID（可选）
     * @param materialId 物料ID（可选）
     * @param warehouseId 仓库ID（可选）
     * @param transactionType 变动类型（可选）
     * @return 分页结果
     */
    IPage<InventoryTransaction> selectTransactionPage(Page<InventoryTransaction> page,
                                                      @Param("inventoryId") Long inventoryId,
                                                      @Param("materialId") Long materialId,
                                                      @Param("warehouseId") Long warehouseId,
                                                      @Param("transactionType") Integer transactionType);
}
