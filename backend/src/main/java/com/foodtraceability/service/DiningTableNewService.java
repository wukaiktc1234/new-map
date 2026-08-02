package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.store.operation.DiningTableQueryDTO;
import com.foodtraceability.entity.DiningTableNew;
import java.util.List;
import java.util.Map;

/**
 * 桌台服务接口
 * 管理桌台的增删改查、状态管理
 */
public interface DiningTableNewService {

    /** 创建桌台 */
    DiningTableNew create(DiningTableNew table);

    /** 更新桌台 */
    DiningTableNew update(DiningTableNew table);

    /** 根据ID获取桌台 */
    DiningTableNew getById(Long tableId);

    /** 根据编码获取桌台 */
    DiningTableNew getByCode(String tableCode);

    /** 查询所有桌台列表 */
    List<DiningTableNew> listAll();

    /** 查询空闲桌台 */
    List<DiningTableNew> listAvailable();

    /** 更新桌台状态 */
    void updateStatus(Long tableId, Integer status, String currentOrderId);

    /** 统计各状态桌台数量 */
    Map<Integer, Long> countByStatus();

    /** 按门店查询桌台列表（分页） */
    IPage<DiningTableNew> listByStoreId(Long storeId, DiningTableQueryDTO query);

    /** 按门店统计桌台 */
    Map<String, Object> getStatsByStoreId(Long storeId);

    /** 检查桌台编码在门店内是否唯一 */
    boolean isTableCodeUnique(Long storeId, String tableCode, Long excludeTableId);
}
