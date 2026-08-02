package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.ProductBasicInfo;
import com.foodtraceability.entity.Product;
import com.foodtraceability.mapper.ProductMapper;
import com.foodtraceability.service.ProductDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 产品数据服务实现类
 * 缓存层已由 Spring Cache（ConcurrentMapCacheManager）通过 @Cacheable/@CacheEvict 注解托管
 */
@Service
public class ProductDataServiceImpl implements ProductDataService {

    private static final Logger log = LoggerFactory.getLogger(ProductDataServiceImpl.class);

    private final ProductMapper productMapper;

    public ProductDataServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public Map<String, ProductBasicInfo> batchGetProductBasicInfo(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return new HashMap<>();
        }

        log.info("批量获取产品基本信息, productIds: {}", productIds);

        // 直接批量查询数据库
        List<Product> products = productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .in(Product::getId, productIds.stream()
                    .map(Long::parseLong)
                    .collect(Collectors.toList()))
        );

        Map<Long, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getId, product -> product));

        Map<String, ProductBasicInfo> resultMap = new HashMap<>();
        for (String productId : productIds) {
            Product product = productMap.get(Long.parseLong(productId));
            resultMap.put(productId, product != null ? convertToBasicInfo(product) : null);
        }

        return resultMap;
    }

    @Override
    @Cacheable(value = "productBasicInfo", key = "#productId", unless = "#result == null")
    public ProductBasicInfo getProductBasicInfo(String productId) {
        if (productId == null || productId.isEmpty()) {
            return null;
        }

        log.info("获取产品基本信息, productId: {}", productId);

        Product product = productMapper.selectById(Long.parseLong(productId));

        if (product == null) {
            log.warn("产品不存在: productId={}", productId);
            return null;
        }

        return convertToBasicInfo(product);
    }

    @Override
    @CacheEvict(value = "productBasicInfo", key = "#productId")
    public void clearProductCache(String productId) {
        // 缓存清理由 @CacheEvict 注解自动处理
    }

    @Override
    public void clearProductBatchCache(List<String> productIds) {
        // 批量缓存清理：Spring Cache 不支持批量 evict，由业务层在更新时逐个调用 clearProductCache
        // 此处保留空实现以维持接口契约
    }

    @Override
    @CacheEvict(value = "productBasicInfo", allEntries = true)
    public void clearAllProductCache() {
        // 缓存清理由 @CacheEvict(allEntries=true) 注解自动处理
    }

    private ProductBasicInfo convertToBasicInfo(Product product) {
        return ProductBasicInfo.builder()
            .productId(product.getId().toString())
            .productName(product.getName())
            .productCode(product.getCode())
            .categoryId(product.getCategoryId() != null ? product.getCategoryId().toString() : null)
            .categoryName(product.getCategoryName())
            .unit(product.getUnit())
            .price(product.getPrice())
            .costPrice(product.getCostPrice())
            .barcode(product.getBarcode())
            .specification(product.getSpecification())
            .origin(product.getOrigin())
            .supplierId(product.getSupplierId() != null ? product.getSupplierId().toString() : null)
            .supplierName(product.getSupplierName())
            .status(product.getStatus())
            .version(1L)
            .updateTime(product.getUpdatedAt() != null ?
                LocalDateTime.ofInstant(product.getUpdatedAt().toInstant(), java.time.ZoneId.systemDefault()) : null)
            .build();
    }

    @Override
    public List<String> getAllProductIds() {
        List<Product> products = productMapper.selectList(null);
        return products.stream()
            .map(product -> product.getId().toString())
            .collect(Collectors.toList());
    }
}
