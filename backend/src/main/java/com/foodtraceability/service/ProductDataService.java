package com.foodtraceability.service;

import com.foodtraceability.dto.ProductBasicInfo;

import java.util.List;
import java.util.Map;

public interface ProductDataService {

    Map<String, ProductBasicInfo> batchGetProductBasicInfo(List<String> productIds);

    ProductBasicInfo getProductBasicInfo(String productId);

    void clearProductCache(String productId);

    void clearProductBatchCache(List<String> productIds);

    void clearAllProductCache();

    List<String> getAllProductIds();
}
