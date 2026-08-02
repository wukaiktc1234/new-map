package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.StoreInventoryLog;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreInventoryLogMapper extends BaseMapper<StoreInventoryLog> {

    IPage<StoreInventoryLog> selectLogPage(Page<StoreInventoryLog> page,
                                           @Param("storeId") String storeId,
                                           @Param("productId") Long productId,
                                           @Param("type") Integer type);
}
