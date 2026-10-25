package com.foodtraceability.security.support;

/**
 * 全局数据范围判定（P3-4 拍板，2026-10-25）。
 *
 * <p>语义：data_scope 的优先级链由 PermissionVerifyServiceImpl 定义——
 * all(100) &gt; company(80) &gt; region(70) &gt; stores/departments(60) &gt; store/department(40) &gt; self(20)。
 * 其中 <b>all 与 company 都是「全公司视角」</b>：all 是超管口径，company 是业务口径
 * （财务总监 / 审计在设计稿 §7.1 矩阵里就是「只读全量」）。</p>
 *
 * <p><b>为什么要有这个类</b>：此前 StoreReconServiceImpl 只认 all，导致
 * finance_director（company）被当成非全局，只能靠硬编码角色白名单兜底；白名单是反模式的
 * （新建任何「全公司」角色都要回来改代码）。现统一走本类，判定只认 data_scope。</p>
 */
public final class GlobalDataScope {

    private GlobalDataScope() {
    }

    /** all 或 company → 全公司视角。 */
    public static boolean isGlobal(String dataScope) {
        if (dataScope == null) {
            return false;
        }
        return "all".equalsIgnoreCase(dataScope) || "company".equalsIgnoreCase(dataScope);
    }

    /** store 或 department（或未知/null）→ 只看本店。 */
    public static boolean isStoreScoped(String dataScope) {
        if (dataScope == null) {
            return true;
        }
        return "store".equalsIgnoreCase(dataScope) || "department".equalsIgnoreCase(dataScope);
    }
}