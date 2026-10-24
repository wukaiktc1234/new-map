package com.foodtraceability.service.purchase;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.entity.Location;
import com.foodtraceability.entity.PurchaseOrder;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.PurchaseOrderMapper;
import com.foodtraceability.mapper.PurchaseStockinMapper;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.service.LocationService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 入库库位解析器（整改 1）：<b>位置归属只允许 ID → ID</b>。
 *
 * <p>历史实现靠 {@code warehouse_code ↔ location_code} 字符串软匹配补账，代码/名称一改名就错位；
 * 本类改为统一的 5 级 ID 链，全部经 {@code location_id_map(src_table/src_id → location_id)} 换算，
 * <b>任何情况下都不做字符串匹配、不返回 null</b>：解析不到直接抛
 * {@link ErrorCode#PARAM_ERROR}，由调用方要求显式指定 locationId。</p>
 *
 * <p>优先序：</p>
 * <ol>
 *   <li>① 调用方显式传入的 locationId（校验存在且未删除）</li>
 *   <li>② 入库单关联采购单 {@code purchase_orders.warehouse_id} → location_id_map('warehouses', id)</li>
 *   <li>③ 采购单 {@code purchase_orders.store_id} → location_id_map('stores_new', id)</li>
 *   <li>④ 追溯码 {@code material_trace_code.store_id} → location_id_map('stores_new', id)</li>
 *   <li>⑤ 当前登录用户 {@code users.location_id}（须存在且未删除）</li>
 * </ol>
 */
@Component
public class StockinLocationResolver {

    private static final Logger log = LoggerFactory.getLogger(StockinLocationResolver.class);

    /** 解析失败统一提示（前端/调用方据此引导显式指定） */
    public static final String RESOLVE_FAILED_MESSAGE = "无法确定入库库位：请在请求中显式指定 locationId";

    private final LocationService locationService;
    private final LocationIdBridge locationIdBridge;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseStockinMapper purchaseStockinMapper;
    private final UserMapper userMapper;

    public StockinLocationResolver(LocationService locationService,
                                   LocationIdBridge locationIdBridge,
                                   PurchaseOrderMapper purchaseOrderMapper,
                                   PurchaseStockinMapper purchaseStockinMapper,
                                   UserMapper userMapper) {
        this.locationService = locationService;
        this.locationIdBridge = locationIdBridge;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseStockinMapper = purchaseStockinMapper;
        this.userMapper = userMapper;
    }

    /** 便捷入口：只给入库单上下文（②③ 由 stockin.order_id 反查采购单） */
    public Long resolveLocationId(Long explicitLocationId, Long stockinId) {
        return resolveLocationId(explicitLocationId, stockinId, null, null);
    }

    /**
     * 解析入库库位。
     *
     * @param explicitLocationId ① 调用方显式指定的库位（可空）
     * @param stockinId          入库单ID（可空；用于反查 order_id）
     * @param orderId            采购单ID（可空；优先于 stockin 反查结果）
     * @param traceStoreId       追溯码上的门店ID字符串（可空；stores_new.store_id）
     * @return 非空的 location_id
     * @throws BusinessException 五级全部解析不到时抛 PARAM_ERROR
     */
    public Long resolveLocationId(Long explicitLocationId, Long stockinId, Long orderId, String traceStoreId) {
        // ① 显式指定：校验存在且未删除（存在性也走 ID，不做 code 匹配）
        if (explicitLocationId != null) {
            Location explicit = locationService.getById(explicitLocationId);
            if (explicit == null || (explicit.getDeleted() != null && explicit.getDeleted() != 0)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "指定的入库库位不存在：locationId=" + explicitLocationId);
            }
            log.debug("入库库位解析命中①显式指定: locationId={}", explicitLocationId);
            return explicitLocationId;
        }

        // 关联采购单：优先入参 orderId，其次由入库单反查
        Long effectiveOrderId = orderId;
        if (effectiveOrderId == null && stockinId != null && purchaseStockinMapper != null) {
            PurchaseStockin stockin = purchaseStockinMapper.selectById(stockinId);
            if (stockin != null) {
                effectiveOrderId = stockin.getOrderId();
            }
        }
        PurchaseOrder order = null;
        if (effectiveOrderId != null && purchaseOrderMapper != null) {
            order = purchaseOrderMapper.selectById(effectiveOrderId);
        }

        // ② 采购单 warehouse_id → 'warehouses'
        if (order != null && order.getWarehouseId() != null) {
            Long byWarehouse = locationIdBridge.toLocationId(LocationIdBridge.SRC_WAREHOUSES, order.getWarehouseId());
            if (byWarehouse != null) {
                log.debug("入库库位解析命中②采购单仓库: orderId={}, warehouseId={}, locationId={}",
                        effectiveOrderId, order.getWarehouseId(), byWarehouse);
                return byWarehouse;
            }
        }

        // ③ 采购单 store_id → 'stores_new'
        if (order != null && order.getStoreId() != null) {
            Long byOrderStore = locationIdBridge.toLocationId(LocationIdBridge.SRC_STORES_NEW, order.getStoreId());
            if (byOrderStore != null) {
                log.debug("入库库位解析命中③采购单门店: orderId={}, storeId={}, locationId={}",
                        effectiveOrderId, order.getStoreId(), byOrderStore);
                return byOrderStore;
            }
        }

        // ④ 追溯码 store_id → 'stores_new'
        Long traceStore = parseLongOrNull(traceStoreId);
        if (traceStore != null) {
            Long byTraceStore = locationIdBridge.toLocationId(LocationIdBridge.SRC_STORES_NEW, traceStore);
            if (byTraceStore != null) {
                log.debug("入库库位解析命中④追溯码门店: storeId={}, locationId={}", traceStore, byTraceStore);
                return byTraceStore;
            }
        }

        // ⑤ 当前登录用户 users.location_id（须存在且未删除）
        Long currentUserId = currentUserIdQuietly();
        if (currentUserId != null && userMapper != null) {
            User user = userMapper.selectById(currentUserId);
            if (user != null && user.getLocationId() != null) {
                Location userLocation = locationService.getById(user.getLocationId());
                if (userLocation != null && (userLocation.getDeleted() == null || userLocation.getDeleted() == 0)) {
                    log.debug("入库库位解析命中⑤当前用户归属: userId={}, locationId={}",
                            currentUserId, user.getLocationId());
                    return user.getLocationId();
                }
            }
        }

        // 全失败：绝不返回 null
        throw new BusinessException(ErrorCode.PARAM_ERROR, RESOLVE_FAILED_MESSAGE);
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 取当前登录用户ID；无 SecurityContext / 未登录时返回 null（不当作错误） */
    private static Long currentUserIdQuietly() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
