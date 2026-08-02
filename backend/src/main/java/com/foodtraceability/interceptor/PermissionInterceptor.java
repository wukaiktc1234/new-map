package com.foodtraceability.interceptor;

import com.foodtraceability.annotation.RequiresPermission;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Permission;
import com.foodtraceability.service.PermissionService;
import com.foodtraceability.service.UserService;
import com.foodtraceability.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

@Component
public class PermissionInterceptor implements HandlerInterceptor {


    public PermissionInterceptor(JwtUtil jwtUtil, UserService userService, PermissionService permissionService, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    private final JwtUtil jwtUtil;

    private final UserService userService;

    private final PermissionService permissionService;

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 安全告警：原代码存在 X-Test-Mode 后门（任意请求可通过 Header 绕过所有权限）和 admin 直接放行（username 可被伪造）
        // 已移除以上两个致命后门，统一基于 Token 中的 permissions 字段进行权限校验

        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            responseError(response, 401, "Unauthorized access");
            return false;
        }

        String jwtToken = token.replace("Bearer ", "");
        Claims claims = jwtUtil.parseJwt(jwtToken);
        if (claims == null) {
            responseError(response, 401, "Invalid token");
            return false;
        }

        Long userId = Long.parseLong(claims.getSubject());
        String username = (String) claims.get("username");

        // 通过 Token 中的 permissions 字段判断管理员通配权限（可信，由 JwtUtils 在登录时签发）
        @SuppressWarnings("unchecked")
        List<String> userPermissions = claims.get("permissions", List.class);
        if (userPermissions != null && userPermissions.contains("*")) {
            return true;
        }

        String positionCode = (String) claims.get("positionCode");
        String departmentId = (String) claims.get("departmentId");
        String levelType = (String) claims.get("levelType");
        String levelId = (String) claims.get("levelId");

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();

            if (method.isAnnotationPresent(RequiresPermission.class)) {
                RequiresPermission requiresPermission = method.getAnnotation(RequiresPermission.class);
                String permissionCode = requiresPermission.value();

                boolean hasPermission = checkPermission(permissionCode, levelType, levelId, departmentId, positionCode, userPermissions);
                if (!hasPermission) {
                    responseError(response, 403, "No operation permission");
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }

    private boolean checkPermission(String permissionCode, String levelType, String levelIdStr, String departmentIdStr, String positionCode, List<String> userPermissions) {
        try {
            if (userPermissions != null) {
                for (String perm : userPermissions) {
                    if ("*".equals(perm)) {
                        return true;
                    }
                    if (perm.equals(permissionCode)) {
                        return true;
                    }
                    if (perm.endsWith(":*") && permissionCode.startsWith(perm.substring(0, perm.length() - 1))) {
                        return true;
                    }
                }
            }

            Long levelId = levelIdStr != null ? Long.parseLong(levelIdStr) : null;
            Long departmentId = departmentIdStr != null ? Long.parseLong(departmentIdStr) : null;

            List<Permission> permissions = permissionService.getUserPermissions(levelType, levelId, departmentId, positionCode);

            for (Permission permission : permissions) {
                if (permission.getPermissionCode().equals(permissionCode) && permission.getStatus() == 1) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void responseError(HttpServletResponse response, int code, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        Result<?> result = Result.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
