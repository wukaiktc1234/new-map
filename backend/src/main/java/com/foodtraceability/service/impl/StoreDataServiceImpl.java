package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.StoreBasicInfo;
import com.foodtraceability.entity.Store;
import com.foodtraceability.mapper.StoreMapper;
import com.foodtraceability.service.StoreDataService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门店数据服务实现类
 * 缓存层已由 Spring Cache（ConcurrentMapCacheManager）通过 @Cacheable/@CacheEvict 注解托管
 */
@Service
public class StoreDataServiceImpl implements StoreDataService {

    private final StoreMapper storeMapper;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param storeMapper 门店Mapper
     */
    public StoreDataServiceImpl(StoreMapper storeMapper) {
        this.storeMapper = storeMapper;
    }

    @Override
    @Cacheable(value = "storeBasicInfo", key = "#storeId", unless = "#result == null")
    public StoreBasicInfo getStoreBasicInfo(String storeId) {
        if (storeId == null || storeId.isEmpty()) {
            return null;
        }

        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            return null;
        }

        return convertToBasicInfo(store);
    }

    @Override
    public Map<String, StoreBasicInfo> batchGetStoreBasicInfo(List<String> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return new HashMap<>();
        }

        // 直接批量查询数据库（单条查询已通过 @Cacheable 提供缓存）
        List<Store> stores = storeMapper.selectList(
            new LambdaQueryWrapper<Store>()
                .in(Store::getStoreId, storeIds)
        );
        Map<String, Store> storeMap = stores.stream()
            .collect(Collectors.toMap(Store::getStoreId, store -> store));

        Map<String, StoreBasicInfo> result = new HashMap<>();
        for (String storeId : storeIds) {
            Store store = storeMap.get(storeId);
            result.put(storeId, store != null ? convertToBasicInfo(store) : null);
        }

        return result;
    }

    @Override
    @CacheEvict(value = "storeBasicInfo", key = "#storeId")
    public void clearStoreCache(String storeId) {
        // 缓存清理由 @CacheEvict 注解自动处理
    }

    @Override
    public void clearStoreBatchCache(List<String> storeIds) {
        // 批量缓存清理：Spring Cache 不支持批量 evict，由业务层在更新时逐个调用 clearStoreCache
        // 此处保留空实现以维持接口契约
    }

    @Override
    @CacheEvict(value = "storeBasicInfo", allEntries = true)
    public void clearAllStoreCache() {
        // 缓存清理由 @CacheEvict(allEntries=true) 注解自动处理
    }

    private StoreBasicInfo convertToBasicInfo(Store store) {
        return StoreBasicInfo.builder()
            .storeId(store.getStoreId())
            .storeName(store.getStoreName())
            .storeCode(store.getStoreCode())
            .address(store.getAddress())
            .phone(store.getPhone())
            .managerId(store.getManagerId())
            .managerName(store.getManagerName())
            .status(store.getStatus())
            .companyId(store.getCompanyId())
            .region(store.getRegion())
            .storeType(store.getStoreType())
            .version(store.getVersion() != null ? store.getVersion().longValue() : 1L)
            .updateTime(store.getUpdatedAt())
            .build();
    }

    @Override
    public List<String> getAllStoreIds() {
        List<Store> stores = storeMapper.selectList(null);
        return stores.stream()
            .map(store -> store.getStoreId())
            .collect(Collectors.toList());
    }
}
