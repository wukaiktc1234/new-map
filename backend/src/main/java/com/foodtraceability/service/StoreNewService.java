package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.store.StoreCreateDTO;
import com.foodtraceability.dto.store.StoreUpdateDTO;
import com.foodtraceability.entity.StoreNew;

import java.util.List;

/**
 * 门店服务接口（增强版）
 */
public interface StoreNewService extends IService<StoreNew> {

    /**
     * 分页查询门店列表
     */
    Page<StoreNew> getPageList(Page<StoreNew> page, String storeName,
                               Integer storeType, Integer status);

    /**
     * 根据编码查询门店
     */
    StoreNew getByStoreCode(String storeCode);

    /**
     * 创建门店
     */
    StoreNew createStore(StoreCreateDTO createDTO);

    /**
     * 更新门店（仅更新非 null 字段）
     */
    StoreNew updateStore(Long storeId, StoreUpdateDTO updateDTO);

    /**
     * 更新门店状态
     */
    void updateStatus(Long storeId, Integer status);

    /**
     * 获取所有营业中的门店
     */
    List<StoreNew> getActiveStores();

    /**
     * 检查门店编码是否可用
     */
    boolean isCodeAvailable(String storeCode, Long excludeId);
}
