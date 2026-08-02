package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.Store;

import java.util.List;

public interface StoreService extends IService<Store> {

    List<Store> getActiveStores();

    Store getStoreById(String id);

    Store createStore(Store store);

    /**
     * 更新门店状态（级联更新关联的部门、职位、员工）
     */
    void updateStoreStatus(String storeId, String status);

    /**
     * 删除门店（级联处理关联的部门、职位、员工）
     */
    void deleteStore(String storeId);
}
