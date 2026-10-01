package com.foodtraceability.common.util;

import com.foodtraceability.common.exception.NoLocationAssignedException;
import com.foodtraceability.common.exception.NoLocationContextException;
import com.foodtraceability.entity.Location;
import com.foodtraceability.service.LocationService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 位置上下文统一守卫（P1-USER-LOCATION-001）。
 *
 * <p><b>目的</b>（design-001 §6 配套硬规则 / design-002 §7.3）：13 处"缺失即兜底"的散写判定
 * 收敛为**一处守卫**，拒绝语义统一为
 * {@link NoLocationAssignedException}（用户归属缺失）/ {@link NoLocationContextException}（对象缺上下文）。
 *
 * <p><b>边界模型</b>（design-002 §7）：
 * <ul>
 *   <li>门店单据（桌台/叫号/门店库存/采购落店/日结…）→ {@link #requireStoreContext()}；
 *       本人归属必须是 <b>STORE</b> 型 location；仓库员工（CENTRAL/DEPOT）被拒绝</li>
 *   <li>仓库单据（入库/出库/损耗/调整/调拨/盘点）→ 走 <b>功能权限</b>（warehouse_manager 角色），
 *       不靠 location 推导；如需显式校验用 {@link #requireWarehouseContext()}</li>
 *   <li>data_scope=all（admin/总部）→ 读侧走 all 分支（**不拒绝**）；
 *       写侧按 design-002 §5 修订二的解析链：显式 locationId → 从对象推导 → 拒绝</li>
 * </ul>
 *
 * <p><b>纪律</b>：null 一律**显式拒绝**，禁止数值兜底（design-001 §6 对 13 处的统一要求）。
 */
@Component
public class LocationGuard {

    private static final Logger log = LoggerFactory.getLogger(LocationGuard.class);

    /** 位置类型：门店（有库存的最小仓库） */
    public static final String TYPE_STORE = "STORE";
    /** 位置类型：中央仓（采购默认落点） */
    public static final String TYPE_CENTRAL = "CENTRAL";
    /** 位置类型：普通仓储仓 */
    public static final String TYPE_DEPOT = "DEPOT";

    private final LocationService locationService;
    private final LocationIdBridge locationIdBridge;

    public LocationGuard(LocationService locationService, LocationIdBridge locationIdBridge) {
        this.locationService = locationService;
        this.locationIdBridge = locationIdBridge;
    }

    /**
     * 当前登录用户的归属 location_id；缺失即 {@code 403}。
     * 用于"只要求有归属、不限定类型"的场景。
     */
    public Long requireCurrentLocationId() {
        Long locationId = SecurityUtils.getCurrentUserLocationId();
        if (locationId == null) {
            throw new NoLocationAssignedException();
        }
        return locationId;
    }

    /**
     * 门店单据上下文：本人归属必须是 <b>STORE</b> 型 location。
     *
     * @return 归属位置换算出的 {@code stores_new.store_id}（存量 store_id 空间消费点直接可用）
     * @throws NoLocationAssignedException 无归属 / 归属非 STORE 型（仓库员工访问门店单据）
     * @throws NoLocationContextException  归属无法经 {@code location_id_map} 换算为门店ID
     */
    public Long requireStoreContext() {
        Long locationId = requireCurrentLocationId();
        Location location = locationService.getById(locationId);
        if (location == null) {
            throw new NoLocationAssignedException(
                    "当前用户归属位置不存在或已停用（location_id=" + locationId + "），请联系管理员");
        }
        if (!TYPE_STORE.equals(location.getLocationType())) {
            log.warn("门店单据拒绝：用户归属非 STORE 型。locationId={}, type={}", locationId, location.getLocationType());
            throw new NoLocationAssignedException(
                    "当前用户归属为仓库位置（" + location.getLocationName() + "），无权操作门店单据");
        }
        Long storeId = locationIdBridge.toStoreId(locationId);
        if (storeId == null) {
            throw new NoLocationContextException(
                    "归属位置无法换算为门店ID（location_id=" + locationId + "）");
        }
        return storeId;
    }

    /**
     * 仓库单据上下文：本人归属必须是 <b>CENTRAL/DEPOT</b> 型 location。
     *
     * @return 归属位置换算出的 {@code warehouses.warehouse_id}
     * @throws NoLocationAssignedException 无归属 / 归属非仓库型（门店员工访问仓库单据）
     * @throws NoLocationContextException  归属无法经 {@code location_id_map} 换算为仓库ID
     */
    public Long requireWarehouseContext() {
        Long locationId = requireCurrentLocationId();
        Location location = locationService.getById(locationId);
        if (location == null) {
            throw new NoLocationAssignedException(
                    "当前用户归属位置不存在或已停用（location_id=" + locationId + "），请联系管理员");
        }
        if (!TYPE_CENTRAL.equals(location.getLocationType()) && !TYPE_DEPOT.equals(location.getLocationType())) {
            log.warn("仓库单据拒绝：用户归属非仓库型。locationId={}, type={}", locationId, location.getLocationType());
            throw new NoLocationAssignedException(
                    "当前用户归属为门店位置（" + location.getLocationName() + "），无权操作仓库单据");
        }
        Long warehouseId = locationIdBridge.toWarehouseId(locationId);
        if (warehouseId == null) {
            throw new NoLocationContextException(
                    "归属位置无法换算为仓库ID（location_id=" + locationId + "）");
        }
        return warehouseId;
    }

    /**
     * 断言：<b>操作对象</b>的位置上下文可解析且与本人归属一致（门店单据写操作）。
     *
     * <p>解析链（design-002 §5 修订二）：① 显式 locationId → ② 从操作对象推导 →
     * ③ 都取不到 → {@link NoLocationContextException}。本方法实现 ①② 的校验与一致性断言。
     *
     * @param targetLocationId 操作对象的显式位置ID（可为 null，表示"由 ② 推导"，此时仅校验本人归属）
     * @return 最终使用的位置ID
     */
    public Long assertStoreContext(Long targetLocationId) {
        Long ownLocationId = requireCurrentLocationId();
        if (targetLocationId != null && !targetLocationId.equals(ownLocationId)) {
            log.warn("门店单据位置上下文不一致：本人={}, 对象={}", ownLocationId, targetLocationId);
            throw new NoLocationAssignedException(
                    "操作对象的位置与当前用户归属不一致（本人 location_id=" + ownLocationId
                            + "，对象 location_id=" + targetLocationId + "）");
        }
        Location location = locationService.getById(ownLocationId);
        if (location == null) {
            throw new NoLocationAssignedException(
                    "当前用户归属位置不存在或已停用（location_id=" + ownLocationId + "），请联系管理员");
        }
        if (!TYPE_STORE.equals(location.getLocationType())) {
            throw new NoLocationAssignedException(
                    "当前用户归属为仓库位置（" + location.getLocationName() + "），无权操作门店单据");
        }
        return ownLocationId;
    }
}
