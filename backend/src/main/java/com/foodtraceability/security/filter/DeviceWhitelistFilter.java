package com.foodtraceability.security.filter;

import com.foodtraceability.service.AppDeviceRegistrationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * APK 设备白名单过滤器
 * 开启后：请求携带 X-Device-Id 头时，必须为已激活（ACTIVE）设备，否则 403 拒绝。
 * 不带设备头的请求（PC 浏览器/管理端）不受影响。
 * 白名单相关接口（status/activate）自身放行。
 */
@Component
public class DeviceWhitelistFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DeviceWhitelistFilter.class);

    private static final String DEVICE_HEADER = "X-Device-Id";

    private final AppDeviceRegistrationService appDeviceRegistrationService;

    @Value("${app.device-whitelist.enabled:true}")
    private boolean whitelistEnabled;

    public DeviceWhitelistFilter(AppDeviceRegistrationService appDeviceRegistrationService) {
        this.appDeviceRegistrationService = appDeviceRegistrationService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // getServletPath 不含 context-path（/api），与 SecurityConfig 的匹配路径一致
        String uri = request.getServletPath();
        // 仅拦截 /v1/** 业务接口
        if (uri == null || !uri.startsWith("/v1/")) {
            return true;
        }
        // 设备白名单自身的接口放行（激活流程需在未激活状态下访问）
        return uri.startsWith("/v1/device-registrations/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!whitelistEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        String deviceId = request.getHeader(DEVICE_HEADER);
        if (deviceId != null && !deviceId.isBlank()) {
            if (!appDeviceRegistrationService.isWhitelisted(deviceId)) {
                log.warn("设备白名单拦截：deviceId={}, uri={}, ip={}",
                        deviceId, request.getServletPath(), request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":40302,\"message\":\"设备未激活或不在白名单，请联系管理员\"}");
                return;
            }
            // 已激活设备：更新最近访问时间（异步不阻塞请求链）
            try {
                appDeviceRegistrationService.touchLastSeen(deviceId);
            } catch (Exception ignored) {
                // 状态更新失败不影响请求
            }
        }
        filterChain.doFilter(request, response);
    }
}
