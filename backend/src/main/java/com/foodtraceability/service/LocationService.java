package com.foodtraceability.service;

import com.foodtraceability.entity.Location;

import java.util.List;

/**
 * 库存位置服务。
 * M1-M2 核心层：只读解析与查询；落账/调拨/员工归属切换在后续批次（M3-M6）。
 */
public interface LocationService {

    /** 按位置ID查询（未删除） */
    Location getById(Long locationId);

    /**
     * 按门店 ID（stores_new.store_id 别名）解析 STORE 型位置。
     * 这是规则 3/4 的标准入口：任何 store_id → location_id 换算必须走本方法。
     * @return 未找到或非活跃时返回 null（调用方按业务决定拒绝语义，禁止数值兜底）
     */
    Location resolveByStoreId(Long storeId);

    /**
     * 按仓库 ID（warehouses.warehouse_id 别名）解析 CENTRAL/DEPOT 型位置。
     */
    Location resolveByWarehouseId(Long warehouseId);

    /**
     * 按位置反查来源仓库 ID（location_id → warehouses.warehouse_id，经 location_id_map，
     * 与 {@link #resolveByWarehouseId(Long)} 对称）。用于向旧空间 warehouse_id 列写值的
     * 存量消费点（C7：InventoryWarningScheduler 预警记录读回）。
     * @return 位置未映射到 warehouses 来源时返回 null（调用方按业务决定语义，禁止数值兜底）
     */
    Long resolveWarehouseIdByLocationId(Long locationId);

    /** 活跃位置列表（可按类型过滤：STORE / CENTRAL / DEPOT / TRANSIT） */
    List<Location> listActive(String locationType);
}
