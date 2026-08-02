package com.foodtraceability.dataservice;

import com.foodtraceability.dto.store.operation.vo.DiningTableVO;

import java.util.List;
import java.util.Map;

/**
 * 桌台数据服务接口
 * 提供桌台的二级缓存(L1本地+L2 Redis)和批量查询功能
 * 缓存键格式: diningTable:basic:{tableId}
 */
public interface DiningTableDataService {

    /**
     * 获取单个桌台基本信息
     *
     * @param tableId 桌台ID
     * @return 桌台视图对象
     */
    DiningTableVO getDiningTableBasicInfo(Long tableId);

    /**
     * 批量获取桌台基本信息
     *
     * @param tableIds 桌台ID列表
     * @return 桌台ID到视图对象的映射
     */
    Map<Long, DiningTableVO> batchGetDiningTableBasicInfo(List<Long> tableIds);

    /**
     * 根据门店ID获取桌台列表
     *
     * @param storeId 门店ID
     * @return 桌台视图对象列表
     */
    List<DiningTableVO> getDiningTablesByStoreId(Long storeId);

    /**
     * 清除指定桌台的缓存
     *
     * @param tableId 桌台ID
     */
    void clearDiningTableCache(Long tableId);

    /**
     * 批量清除桌台缓存
     *
     * @param tableIds 桌台ID列表
     */
    void clearDiningTableBatchCache(List<Long> tableIds);

    /**
     * 清除指定门店下所有桌台的缓存
     *
     * @param storeId 门店ID
     */
    void clearStoreDiningTableCache(Long storeId);
}
