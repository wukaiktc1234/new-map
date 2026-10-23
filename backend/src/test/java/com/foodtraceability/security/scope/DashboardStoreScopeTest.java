package com.foodtraceability.security.scope;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foodtraceability.common.util.LocationIdBridge;
import com.foodtraceability.config.DashboardStoreScopeProperties;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.SalesOrder;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

/**
 * P1-DASHBOARD-FILTER-001（R8）看板门店作用域单测。
 *
 * <p>锁定的产品约束：<b>开关式、默认关闭、行为零变化</b>。
 * 开关关闭或解析不到门店时，生成的查询条件必须与历史版本一致（不追加任何列）。</p>
 */
class DashboardStoreScopeTest {

    /** 测试用普通用户（非 admin，JWT 携带归属门店位置）。 */
    private static final Long USER_ID = 7L;

    /** 用户归属位置ID（location_id 空间）。 */
    private static final Long LOCATION_ID = 2002L;

    /** 该位置对应的真实门店ID（stores_new.store_id）。 */
    private static final Long STORE_ID = 2L;

    private DashboardStoreScopeProperties properties;
    private UserService userService;
    private EmployeeMapper employeeMapper;
    private LocationIdBridge locationIdBridge;
    private DashboardStoreScopeResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new DashboardStoreScopeProperties();
        userService = Mockito.mock(UserService.class);
        employeeMapper = Mockito.mock(EmployeeMapper.class);
        locationIdBridge = Mockito.mock(LocationIdBridge.class);
        resolver = new DashboardStoreScopeResolver(properties, userService, employeeMapper, locationIdBridge);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------ 用例

    @Test
    @DisplayName("a) 开关关闭 → 不追加过滤，查询条件与旧实现一致（空条件）")
    void flagOffKeepsLegacyBehaviour() {
        properties.setEnabled(false);
        bindLoginUser(false, LOCATION_ID);

        assertFalse(resolver.isEnabled(), "开关应保持关闭");
        assertTrue(resolver.currentStoreId().isEmpty(), "开关关闭时必须返回 empty（不限门店）");

        QueryWrapper<SalesOrder> wrapper = resolver.applyStoreId(new QueryWrapper<>());
        assertFalse(wrapper.getSqlSegment().contains("store_id"), "不得追加 store_id 条件，实际: " + wrapper.getSqlSegment());
        assertTrue(wrapper.getParamNameValuePairs().isEmpty(), "条件参数应为空（与旧实现一致）");

        QueryWrapper<SalesOrder> locationWrapper = resolver.applyLocationId(new QueryWrapper<>());
        assertFalse(locationWrapper.getSqlSegment().contains("location_id"),
                "不得追加 location_id 条件，实际: " + locationWrapper.getSqlSegment());

        Mockito.verifyNoInteractions(locationIdBridge, userService, employeeMapper);
    }

    @Test
    @DisplayName("b) 开关开启 + 用户绑门店 2 → 条件含 store_id=2")
    void enabledUserBoundToStoreAddsCondition() {
        properties.setEnabled(true);
        bindLoginUser(false, LOCATION_ID);
        Mockito.when(locationIdBridge.toStoreId(LOCATION_ID)).thenReturn(STORE_ID);

        Optional<Long> storeId = resolver.currentStoreId();
        assertEquals(Optional.of(STORE_ID), storeId, "必须解析出门店 2");

        QueryWrapper<SalesOrder> wrapper = resolver.applyStoreId(new QueryWrapper<>());
        assertTrue(wrapper.getSqlSegment().contains("store_id"), "必须追加 store_id 条件，实际: " + wrapper.getSqlSegment());
        assertTrue(wrapper.getParamNameValuePairs().containsValue(STORE_ID),
                "条件参数必须含门店 2，实际: " + wrapper.getParamNameValuePairs());

        // JWT 已带 locationId，不应回查 users/employees
        Mockito.verifyNoInteractions(userService, employeeMapper);
    }

    @Test
    @DisplayName("c) admin 用户 → 不追加过滤（全量口径）")
    void adminGetsNoFilter() {
        properties.setEnabled(true);
        bindLoginUser(true, LOCATION_ID);

        assertTrue(resolver.currentStoreId().isEmpty(), "admin 必须返回 empty（全量）");

        QueryWrapper<SalesOrder> wrapper = resolver.applyStoreId(new QueryWrapper<>());
        assertFalse(wrapper.getSqlSegment().contains("store_id"), "admin 不得追加门店条件");
        assertTrue(wrapper.getParamNameValuePairs().isEmpty());

        Mockito.verifyNoInteractions(locationIdBridge, userService, employeeMapper);
    }

    @Test
    @DisplayName("d) 用户无门店绑定 → 不追加 + WARN 日志")
    void userWithoutBindingLogsWarnAndSkipsFilter() {
        properties.setEnabled(true);
        bindLoginUser(false, null);

        User dbUser = new User();
        dbUser.setId(USER_ID);
        dbUser.setLocationId(null);
        Mockito.when(userService.getById(USER_ID)).thenReturn(dbUser);
        Mockito.when(employeeMapper.selectOne(any())).thenReturn(null);

        Logger resolverLogger = (Logger) LoggerFactory.getLogger(DashboardStoreScopeResolver.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        resolverLogger.addAppender(appender);
        try {
            assertTrue(resolver.currentStoreId().isEmpty(), "无绑定时必须返回 empty（不限门店）");

            QueryWrapper<SalesOrder> wrapper = resolver.applyStoreId(new QueryWrapper<>());
            assertFalse(wrapper.getSqlSegment().contains("store_id"), "无绑定时不得追加门店条件");

            List<ILoggingEvent> warnings = appender.list.stream()
                    .filter(event -> event.getLevel() == Level.WARN)
                    .toList();
            assertTrue(warnings.stream().anyMatch(event -> event.getFormattedMessage().contains("不限门店")),
                    "必须留下 WARN 日志（缺绑定 → 不限门店，仍可能越权），实际: " + appender.list);
        } finally {
            resolverLogger.detachAppender(appender);
        }
    }

    @Test
    @DisplayName("d2) JWT 无 locationId 时回落到 employees.location_id（employees.user_id 关联）")
    void fallsBackToEmployeeLocation() {
        properties.setEnabled(true);
        bindLoginUser(false, null);

        Mockito.when(userService.getById(USER_ID)).thenReturn(null);
        Employee employee = new Employee();
        employee.setLocationId(LOCATION_ID);
        Mockito.when(employeeMapper.selectOne(any())).thenReturn(employee);
        Mockito.when(locationIdBridge.toStoreId(LOCATION_ID)).thenReturn(STORE_ID);

        assertEquals(Optional.of(STORE_ID), resolver.currentStoreId(), "必须经 employees.location_id 解析出门店 2");

        QueryWrapper<SalesOrder> wrapper = resolver.applyStoreId(new QueryWrapper<>());
        // MyBatis-Plus 的参数是惰性登记的：先渲染 SQL 片段，再看参数表
        assertTrue(wrapper.getSqlSegment().contains("store_id"), "必须追加 store_id 条件");
        assertTrue(wrapper.getParamNameValuePairs().containsValue(STORE_ID),
                "条件参数必须含门店 2，实际: " + wrapper.getParamNameValuePairs());
    }

    @Test
    @DisplayName("e) 有归属但无 stores_new 映射（如仓库）→ 门店维度不追加，位置维度仍生效")
    void locationWithoutStoreMappingKeepsStoreUnfiltered() {
        properties.setEnabled(true);
        bindLoginUser(false, LOCATION_ID);
        Mockito.when(locationIdBridge.toStoreId(LOCATION_ID)).thenReturn(null);

        assertTrue(resolver.currentStoreId().isEmpty(), "无门店映射时 store 维度必须 empty");
        assertEquals(Optional.of(LOCATION_ID), resolver.currentLocationId(), "location 维度仍应返回归属位置");

        QueryWrapper<SalesOrder> storeWrapper = resolver.applyStoreId(new QueryWrapper<>());
        assertFalse(storeWrapper.getSqlSegment().contains("store_id"));

        QueryWrapper<SalesOrder> locationWrapper = resolver.applyLocationId(new QueryWrapper<>());
        assertTrue(locationWrapper.getSqlSegment().contains("location_id"), "位置维度必须追加 location_id 条件");
        assertTrue(locationWrapper.getParamNameValuePairs().containsValue(LOCATION_ID),
                "条件参数必须含归属位置 2002，实际: " + locationWrapper.getParamNameValuePairs());
    }

    // ------------------------------------------------ 工具

    private void bindLoginUser(boolean admin, Long locationId) {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setUserId(String.valueOf(USER_ID));
        securityUser.setUsername("store_clerk_7");
        securityUser.setRoles(admin ? List.of("admin") : List.of("store_clerk"));
        securityUser.setLocationId(locationId);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
