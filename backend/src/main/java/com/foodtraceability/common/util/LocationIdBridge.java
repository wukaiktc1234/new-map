package com.foodtraceability.common.util;

import com.foodtraceability.mapper.LocationIdMapMapper;
import org.springframework.stereotype.Component;

/**
 * 旧 ID ↔ location_id 换算桥（规则 3/4）。
 *
 * <p><b>背景（P1-USER-LOCATION-001）</b>：{@code users.store_id → users.location_id} 改名后，
 * 该列的 ID 空间由 {@code stores_new.store_id} 变为 {@code locations.location_id}。
 * 凡"把用户归属当 store_id 用"的消费点（按 {@code store_id} 列过滤/落库/查门店名等）
 * <b>必须经本桥反查 stores_new.store_id</b>；严禁假设两个 ID 空间数值相等
 * （见 {@code location-organization-separation-design-002.md §7}）。
 *
 * <p>本组件同时暴露<b>静态入口</b>，供 {@code SecurityUtils} 等静态工具使用
 * （组件构造时自注册）。非 Spring 上下文（如纯单测）下静态入口返回 {@code null}，
 * 调用方按"未分配归属"语义处理，不做数值兜底。
 */
@Component
public class LocationIdBridge {

    /** 来源表名：门店（stores_new.store_id） */
    public static final String SRC_STORES_NEW = "stores_new";

    /** 来源表名：仓库（warehouses.warehouse_id） */
    public static final String SRC_WAREHOUSES = "warehouses";

    private static volatile LocationIdBridge instance;

    private final LocationIdMapMapper locationIdMapMapper;

    public LocationIdBridge(LocationIdMapMapper locationIdMapMapper) {
        this.locationIdMapMapper = locationIdMapMapper;
        instance = this;
    }

    /** location_id → stores_new.store_id；无映射返回 null（禁止数值兜底） */
    public Long toStoreId(Long locationId) {
        if (locationId == null) {
            return null;
        }
        return locationIdMapMapper.selectSrcIdByLocationId(SRC_STORES_NEW, locationId);
    }

    /**
     * stores_new.store_id → location_id；无映射返回 null。
     *
     * <p>保留原签名（零调用点冲击），实现委托给泛化入口
     * （P1-USER-LOCATION-002 D1-1）。
     */
    public Long toLocationId(Long storeId) {
        return toLocationId(SRC_STORES_NEW, storeId);
    }

    /**
     * 泛化正向入口：{@code (srcTable, srcId) → locations.location_id}；无映射返回 null。
     *
     * <p><b>P1-USER-LOCATION-002 缺口①</b>：此前桥只有 {@code location_id → warehouse_id}
     * 的反向入口（{@link #toWarehouseId}），没有 {@code warehouse_id → location_id} 的正向入口，
     * 导致"选仓库"的员工归属无法换算成 {@code location_id}（{@code employees.location_id} 恒 NULL）。
     *
     * <p>无映射时返回 {@code null} 而非兜底值：未纳入位置体系的来源（如 P1-LOCATION-MODEL-001
     * 「全清」决策排除的 8 个 E2E/TEST 仓库）按显式拒绝语义处理，禁止数值/名称兜底。
     */
    public Long toLocationId(String srcTable, Long srcId) {
        if (srcTable == null || srcTable.isEmpty() || srcId == null) {
            return null;
        }
        return locationIdMapMapper.selectLocationIdBySrcId(srcTable, srcId);
    }

    /** location_id → warehouses.warehouse_id；无映射返回 null */
    public Long toWarehouseId(Long locationId) {
        if (locationId == null) {
            return null;
        }
        return locationIdMapMapper.selectSrcIdByLocationId(SRC_WAREHOUSES, locationId);
    }

    /** 静态入口：location_id → stores_new.store_id（供 SecurityUtils 等静态工具） */
    public static Long storeIdOf(Long locationId) {
        LocationIdBridge bridge = instance;
        return bridge == null ? null : bridge.toStoreId(locationId);
    }

    /** 静态入口：stores_new.store_id → location_id */
    public static Long locationIdOfStore(Long storeId) {
        LocationIdBridge bridge = instance;
        return bridge == null ? null : bridge.toLocationId(storeId);
    }

    /**
     * 静态入口：warehouses.warehouse_id → location_id（与 {@link #locationIdOfStore} 对称）。
     *
     * <p>P1-USER-LOCATION-002 D1-1 新增；非 Spring 上下文返回 {@code null}，调用方按
     * "该来源未纳入位置体系"语义处理。
     */
    public static Long locationIdOfWarehouse(Long warehouseId) {
        LocationIdBridge bridge = instance;
        return bridge == null ? null : bridge.toLocationId(SRC_WAREHOUSES, warehouseId);
    }
}
