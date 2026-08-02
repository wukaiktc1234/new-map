package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.InventoryTransfer;

/**
 * 调拨单服务接口
 * 定义调拨管理相关的业务方法
 */
public interface InventoryTransferService {
    
    /**
     * 创建调拨单
     */
    InventoryTransfer createInventoryTransfer(InventoryTransfer inventoryTransfer);
    
    /**
     * 更新调拨单
     */
    InventoryTransfer updateInventoryTransfer(Long id, InventoryTransfer inventoryTransfer);
    
    /**
     * 根据ID获取调拨单
     */
    InventoryTransfer getInventoryTransferById(Long id);
    
    /**
     * 根据ID删除调拨单
     */
    void deleteInventoryTransfer(Long id);
    
    /**
     * 分页查询调拨单列表
     */
    IPage<InventoryTransfer> getInventoryTransferPage(Page<InventoryTransfer> page, String transferNo, Long fromWarehouseId, Long toWarehouseId, String status, String applyTimeStart, String applyTimeEnd);
    
    /**
     * 审批调拨单
     */
    InventoryTransfer approveInventoryTransfer(Long id, String status, String remark);
    
    /**
     * 执行调拨
     */
    InventoryTransfer executeInventoryTransfer(Long id);
}