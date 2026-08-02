package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.StoreInventoryLog;

public interface StoreInventoryLogService {

    StoreInventoryLog createLog(StoreInventoryLog log);

    IPage<StoreInventoryLog> getLogPage(Page<StoreInventoryLog> page, String storeId, Long productId, Integer type);
}
