package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventorySummary;

public interface InventorySummaryService {

    IPage<InventorySummary> getSummaryPage(Page<InventorySummary> page, String productName);
}
