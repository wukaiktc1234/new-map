package com.foodtraceability.service;

import com.foodtraceability.dto.StoreBasicInfo;

import java.util.List;
import java.util.Map;

public interface StoreDataService {

    Map<String, StoreBasicInfo> batchGetStoreBasicInfo(List<String> storeIds);

    StoreBasicInfo getStoreBasicInfo(String storeId);

    void clearStoreCache(String storeId);

    void clearStoreBatchCache(List<String> storeIds);

    void clearAllStoreCache();

    List<String> getAllStoreIds();
}
