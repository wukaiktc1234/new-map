package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Warehouse;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 仓库Mapper接口
 * 提供仓库相关的数据库操作
 */
@Repository
public interface WarehouseMapper extends BaseMapper<Warehouse> {

    /**
     * 分页查询仓库列表
     *
     * @param page 分页对象
     * @param warehouseName 仓库名称（可选，模糊查询）
     * @param warehouseCode 仓库编码（可选，模糊查询）
     * @param warehouseType 仓库类型（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    IPage<Warehouse> selectWarehousePage(Page<Warehouse> page,
                                         @Param("warehouseName") String warehouseName,
                                         @Param("warehouseCode") String warehouseCode,
                                         @Param("warehouseType") Integer warehouseType,
                                         @Param("status") Integer status);
}
