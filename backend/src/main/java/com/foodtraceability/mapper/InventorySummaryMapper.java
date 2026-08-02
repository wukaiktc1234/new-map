package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventorySummary;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventorySummaryMapper extends BaseMapper<InventorySummary> {

    IPage<InventorySummary> selectSummaryPage(Page<InventorySummary> page,
                                              @Param("productName") String productName);
}
