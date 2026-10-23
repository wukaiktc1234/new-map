package com.foodtraceability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 看板门店级数据过滤开关（P1-DASHBOARD-FILTER-001 / R8）。
 *
 * <p><b>默认关闭</b>：{@code app.dashboard.store-scope.enabled=false} 时，
 * {@code DashboardServiceImpl} 的 5 个查询入口不追加任何门店条件，行为与历史版本逐字一致。</p>
 *
 * <p><b>开启前必须确认</b> {@code users.location_id} / {@code employees.location_id} 数据齐全：
 * 解析不到归属的用户按「不限门店」处理（与现状一致），仍可能越权。</p>
 */
@Component
@ConfigurationProperties(prefix = "app.dashboard.store-scope")
public class DashboardStoreScopeProperties {

    /** 是否启用看板门店级数据过滤，默认 false（行为零变化）。 */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
