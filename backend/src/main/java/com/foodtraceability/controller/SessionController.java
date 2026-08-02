package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.SessionDeviceResponse;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "会话管理", description = "多设备登录管理接口")
@RestController
@RequestMapping("/v1/sessions")
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);


    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    private final SessionService sessionService;

    @Operation(summary = "获取当前用户的所有登录设备")
    @GetMapping
    public Result<List<SessionDeviceResponse>> getUserSessions(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            if (currentUser == null) {
                return Result.error(401, "未认证");
            }
            
            String userId = currentUser.getUserId();
            String currentTokenId = extractTokenId(authorizationHeader);
            
            List<SessionDeviceResponse> sessions = sessionService.getUserSessions(userId);
            
            sessions.forEach(session -> {
                session.setIsCurrentDevice(session.getSessionId().equals(currentTokenId));
            });
            
            return Result.success(sessions);
        } catch (Exception e) {
            logger.error("获取会话列表失败", e);
            return Result.error("获取会话列表失败");
        }
    }

    @Operation(summary = "踢出指定设备")
    @DeleteMapping("/{sessionId}")
    public Result<String> kickSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal SecurityUser currentUser) {
        try {
            if (currentUser == null) {
                return Result.error(401, "未认证");
            }
            
            sessionService.removeSession(currentUser.getUserId(), sessionId);
            logger.info("踢出设备: userId={}, sessionId={}", currentUser.getUserId(), sessionId);
            
            return Result.success("设备已踢出");
        } catch (Exception e) {
            logger.error("踢出设备失败", e);
            return Result.error("踢出设备失败");
        }
    }

    @Operation(summary = "踢出所有其他设备")
    @DeleteMapping("/others")
    public Result<Map<String, Integer>> kickAllOtherSessions(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            if (currentUser == null) {
                return Result.error(401, "未认证");
            }
            
            String currentTokenId = extractTokenId(authorizationHeader);
            
            List<SessionDeviceResponse> sessions = sessionService.getUserSessions(currentUser.getUserId());
            int count = (int) sessions.stream()
                .filter(s -> !s.getSessionId().equals(currentTokenId))
                .count();
            
            sessionService.removeAllOtherSessions(currentUser.getUserId(), currentTokenId);
            logger.info("踢出所有其他设备: userId={}, count={}", currentUser.getUserId(), count);
            
            return Result.success(Map.of("removedCount", count));
        } catch (Exception e) {
            logger.error("踢出所有其他设备失败", e);
            return Result.error("踢出所有其他设备失败");
        }
    }

    private String extractTokenId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authorizationHeader.substring(7);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);
        } catch (Exception e) {
            return authorizationHeader.substring(7).hashCode() + "";
        }
    }
}
