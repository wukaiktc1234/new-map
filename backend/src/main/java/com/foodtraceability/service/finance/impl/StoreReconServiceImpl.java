package com.foodtraceability.service.finance.impl;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.dto.finance.recon.StoreReconDailyVO;
import com.foodtraceability.service.PermissionVerifyService;
import com.foodtraceability.service.finance.StoreReconService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 门店对账视图（AC-49）—— **纯只读**实现。
 *
 * <p><b>为什么自带一套范围判定而不是直接用 DashboardStoreScopeResolver</b>：
 * 看板解析器（{@code com.foodtraceability.security.scope.DashboardStoreScopeResolver}）的契约是
 * 「返回 empty = **不限门店**」，且受 {@code app.dashboard.store-scope.enabled} 开关控制（默认关闭）；
 * 它的空值语义是「放行」，而 AC-49 要求空值「拒绝」。直接复用会把开关关闭时的店长变成全量可见，
 * 属于越权面。因此这里复用**同一套底层原语**（{@link PermissionVerifyService#getUserDataScope} +
 * {@link SecurityUtils#getCurrentUserLocationId} + {@link LocationIdBridge#storeIdOf}），
 * 但把空值语义收紧为 403（与 SecurityUtils 的「null = 显式拒绝」注释一致）。</p>
 */
@Service
public class StoreReconServiceImpl implements StoreReconService {

    private static final Logger log = LoggerFactory.getLogger(StoreReconServiceImpl.class);

    /** 全局数据范围（可查任意店）——与 permissionVerifyService.getUserDataScope 的返回值口径一致 */
    private static final String SCOPE_ALL = "all";

    /**
     * 全局视角角色（AC-49 指定）：财务总监 / 财务经理 / 审计。
     *
     * <p>为什么不能只看 {@code roles.data_scope}：种子数据里这些角色行的 data_scope 未必是 'all'
     * （实测 qa_admin 的 data_scope 不是 all，会把自己锁在 403 里）。按**角色白名单**兜底判定，
     * 与 lead 的 AC-49 口径「FINANCE_DIRECTOR/FINANCE_MANAGER/AUDITOR → 任意店」一致；
     * 角色码统一走 RoleCodeNormalizer 归一，避免 ROLE_ 前缀/大小写差异。</p>
     */
    private static final java.util.Set<String> ALL_SCOPE_ROLES =
            java.util.Set.of("finance_director", "finance_manager", "auditor", "admin");

    private final JdbcTemplate jdbc;
    private final PermissionVerifyService permissionVerifyService;

    public StoreReconServiceImpl(JdbcTemplate jdbc, PermissionVerifyService permissionVerifyService) {
        this.jdbc = jdbc;
        this.permissionVerifyService = permissionVerifyService;
    }

    @Override
    @Transactional(readOnly = true)
    public StoreReconDailyVO daily(Long storeId, LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        Long effectiveStoreId = resolveEffectiveStoreId(storeId);

        StoreReconDailyVO vo = new StoreReconDailyVO();
        vo.storeId = effectiveStoreId;
        vo.businessDate = day;
        vo.expectedAmount = 0L;
        vo.actualAmount = 0L;
        vo.differenceAmount = 0L;
        vo.slipCount = 0;

        // 只读：完全依赖 deposit_slips 既有列（expected_amount / actual_amount / difference_amount，单位分）
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT slip_id, slip_no, status, expected_amount, actual_amount, difference_amount, remark "
                        + "  FROM deposit_slips "
                        + " WHERE store_id = ? AND business_date = ? AND deleted = 0 "
                        + " ORDER BY slip_id DESC",
                effectiveStoreId, day);
        if (rows.isEmpty()) {
            return vo;
        }
        long expected = 0L;
        long actual = 0L;
        long diff = 0L;
        for (Map<String, Object> r : rows) {
            expected += num(r.get("expected_amount"));
            actual += num(r.get("actual_amount"));
            diff += num(r.get("difference_amount"));
        }
        Map<String, Object> latest = rows.get(0);
        vo.expectedAmount = expected;
        vo.actualAmount = actual;
        vo.differenceAmount = diff;
        vo.slipId = latest.get("slip_id") == null ? null : ((Number) latest.get("slip_id")).longValue();
        vo.slipNo = (String) latest.get("slip_no");
        vo.slipStatus = (String) latest.get("status");
        vo.remark = (String) latest.get("remark");
        vo.slipCount = rows.size();
        log.info("[STORE_RECON] storeId={} businessDate={} slips={} expected={} actual={} diff={}",
                effectiveStoreId, day, rows.size(), expected, actual, diff);
        return vo;
    }

    /**
     * 计算「本次查询真正生效的门店」——所有拒绝都在这里发生（403，绝不 404、绝不返回空集）。
     */
    private Long resolveEffectiveStoreId(Long requestedStoreId) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw forbidden("未登录或身份上下文缺失，无权访问门店对账数据");
        }
        String userIdStr = String.valueOf(userId);
        String dataScope = permissionVerifyService.getUserDataScope(userIdStr);
        boolean allScope = SCOPE_ALL.equalsIgnoreCase(dataScope)
                || permissionVerifyService.isAdmin(userIdStr)
                || hasAllScopeRole(userIdStr);

        if (allScope) {
            if (requestedStoreId == null) {
                // 全局用户也必须显式指定门店：避免「不传就查全公司合计」这种隐式放量
                throw forbidden("全局数据范围用户请显式指定 storeId");
            }
            return requestedStoreId;
        }

        // 非全局：只能看自己归属门店
        Long locationId = SecurityUtils.getCurrentUserLocationId();
        Long ownStoreId = locationId == null ? null : LocationIdBridge.storeIdOf(locationId);
        if (ownStoreId == null) {
            throw forbidden("当前用户未分配归属门店，无权访问门店对账数据");
        }
        if (requestedStoreId != null && !requestedStoreId.equals(ownStoreId)) {
            throw forbidden("无权访问其他门店数据（当前门店=" + ownStoreId
                    + "，请求门店=" + requestedStoreId + "，dataScope=" + dataScope + "）");
        }
        return ownStoreId;
    }

    /** 是否持有全局视角角色（FINANCE_DIRECTOR / FINANCE_MANAGER / AUDITOR / ADMIN） */
    private boolean hasAllScopeRole(String userIdStr) {
        java.util.Set<String> roles = permissionVerifyService.getUserRoles(userIdStr);
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String role : roles) {
            if (role == null) {
                continue;
            }
            String normalized = com.foodtraceability.security.utils.RoleCodeNormalizer.normalize(role);
            if (normalized != null && ALL_SCOPE_ROLES.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static long num(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    private static BusinessException forbidden(String msg) {
        return new BusinessException(ErrorCode.FORBIDDEN.getCode(), msg);
    }
}
