package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.InventorySummary;
import com.foodtraceability.mapper.InventorySummaryMapper;
import com.foodtraceability.service.InventorySummaryService;
import org.springframework.stereotype.Service;

@Service
public class InventorySummaryServiceImpl extends ServiceImpl<InventorySummaryMapper, InventorySummary> implements InventorySummaryService {


    public InventorySummaryServiceImpl(InventorySummaryMapper inventorySummaryMapper) {
        this.inventorySummaryMapper = inventorySummaryMapper;
    }

    private final InventorySummaryMapper inventorySummaryMapper;

    @Override
    public IPage<InventorySummary> getSummaryPage(Page<InventorySummary> page, String productName) {
        return inventorySummaryMapper.selectSummaryPage(page, productName);
    }
}
