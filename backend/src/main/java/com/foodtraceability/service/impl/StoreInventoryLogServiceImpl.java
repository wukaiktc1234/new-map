package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.StoreInventoryLog;
import com.foodtraceability.mapper.StoreInventoryLogMapper;
import com.foodtraceability.service.StoreInventoryLogService;
import com.foodtraceability.utils.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class StoreInventoryLogServiceImpl extends ServiceImpl<StoreInventoryLogMapper, StoreInventoryLog> implements StoreInventoryLogService {


    public StoreInventoryLogServiceImpl(StoreInventoryLogMapper storeInventoryLogMapper) {
        this.storeInventoryLogMapper = storeInventoryLogMapper;
    }

    private final StoreInventoryLogMapper storeInventoryLogMapper;

    @Override
    public StoreInventoryLog createLog(StoreInventoryLog log) {
        this.save(log);
        return log;
    }

    @Override
    public IPage<StoreInventoryLog> getLogPage(Page<StoreInventoryLog> page, String storeId, Long productId, Integer type) {
        String actualStoreId = storeId;
        
        if (!SecurityUtils.isAdmin() && SecurityUtils.hasStorePermission()) {
            String currentUserStoreId = SecurityUtils.getCurrentUserStoreId();
            if (currentUserStoreId != null && !currentUserStoreId.isEmpty()) {
                actualStoreId = currentUserStoreId;
            }
        }
        
        return storeInventoryLogMapper.selectLogPage(page, actualStoreId, productId, type);
    }
}
