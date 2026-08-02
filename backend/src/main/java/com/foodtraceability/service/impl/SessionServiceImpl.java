package com.foodtraceability.service.impl;

import com.foodtraceability.dto.SessionDeviceResponse;
import com.foodtraceability.security.config.JwtConfig;
import com.foodtraceability.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 会话服务实现类
 *
 * 重构说明：
 * - 已移除 Redis 依赖（RedisTemplate）
 * - 改为内存 ConcurrentHashMap 存储会话信息
 * - 通过会话过期时间字段实现 TTL，惰性清理过期会话
 */
@Service
public class SessionServiceImpl implements SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionServiceImpl.class);

    /**
     * 会话数据条目，存储会话信息和过期时间
     */
    private static final class SessionEntry {
        final String sessionId;
        final String userId;
        final String token;
        final String deviceInfo;
        final String ipAddress;
        final LocalDateTime loginTime;
        volatile LocalDateTime lastActivityTime;
        final LocalDateTime expiresAt;

        SessionEntry(String sessionId, String userId, String token, String deviceInfo,
                     String ipAddress, LocalDateTime loginTime, LocalDateTime expiresAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.token = token;
            this.deviceInfo = deviceInfo;
            this.ipAddress = ipAddress;
            this.loginTime = loginTime;
            this.lastActivityTime = loginTime;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /** 会话存储：sessionId -> 会话条目 */
    private final ConcurrentHashMap<String, SessionEntry> sessionStore = new ConcurrentHashMap<>();
    /** 用户会话索引：userId -> 该用户的会话ID集合 */
    private final ConcurrentHashMap<String, Set<String>> userSessionIndex = new ConcurrentHashMap<>();

    private final JwtConfig jwtConfig;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param jwtConfig JWT配置
     */
    public SessionServiceImpl(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public void registerSession(String userId, String token, String deviceInfo, String ipAddress) {
        try {
            String tokenId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            long expirationMs = jwtConfig.getAccessTokenExpiration();
            LocalDateTime expiresAt = now.plus(expirationMs, ChronoUnit.MILLIS);

            SessionEntry entry = new SessionEntry(tokenId, userId, token, deviceInfo, ipAddress, now, expiresAt);
            sessionStore.put(tokenId, entry);

            // 加入用户会话索引
            userSessionIndex.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(tokenId);

            logger.info("会话已注册: userId={}, sessionId={}, device={}", userId, tokenId, deviceInfo);
        } catch (Exception ex) {
            logger.warn("会话注册失败，不影响登录流程: {}", ex.getMessage());
        }
    }

    @Override
    public void removeSession(String userId, String tokenId) {
        sessionStore.remove(tokenId);
        Set<String> userSessions = userSessionIndex.get(userId);
        if (userSessions != null) {
            userSessions.remove(tokenId);
            if (userSessions.isEmpty()) {
                userSessionIndex.remove(userId);
            }
        }
        logger.info("会话已移除: userId={}, sessionId={}", userId, tokenId);
    }

    @Override
    public void removeSessionByToken(String userId, String token) {
        if (userId == null || token == null) {
            return;
        }

        try {
            Set<String> sessionIds = userSessionIndex.get(userId);
            if (sessionIds == null || sessionIds.isEmpty()) {
                return;
            }

            // 遍历用户的所有会话，查找与 token 匹配的会话并移除
            for (String sessionId : sessionIds) {
                SessionEntry entry = sessionStore.get(sessionId);
                if (entry != null && token.equals(entry.token)) {
                    removeSession(userId, sessionId);
                    logger.info("按令牌移除会话成功: userId={}, sessionId={}", userId, sessionId);
                    return;
                }
            }
            logger.debug("未找到匹配的会话记录: userId={}", userId);
        } catch (Exception e) {
            logger.error("按令牌移除会话失败: userId={}", userId, e);
        }
    }

    @Override
    public void removeAllOtherSessions(String userId, String currentTokenId) {
        List<SessionDeviceResponse> sessions = getUserSessions(userId);
        int removedCount = 0;

        for (SessionDeviceResponse session : sessions) {
            if (!session.getSessionId().equals(currentTokenId)) {
                removeSession(userId, session.getSessionId());
                removedCount++;
            }
        }

        logger.info("已踢出其他设备: userId={}, count={}", userId, removedCount);
    }

    @Override
    public List<SessionDeviceResponse> getUserSessions(String userId) {
        Set<String> sessionIds = userSessionIndex.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return new ArrayList<>();
        }

        return sessionIds.stream()
            .map(sessionId -> {
                SessionEntry entry = sessionStore.get(sessionId);
                if (entry == null || entry.isExpired()) {
                    // 惰性清理过期会话
                    if (entry != null) {
                        removeSession(userId, sessionId);
                    }
                    return null;
                }
                SessionDeviceResponse response = new SessionDeviceResponse();
                response.setSessionId(entry.sessionId);
                response.setDeviceInfo(entry.deviceInfo);
                response.setIpAddress(entry.ipAddress);
                response.setLoginTime(entry.loginTime);
                response.setLastActivityTime(entry.lastActivityTime);
                response.setExpiresAt(entry.expiresAt);
                return response;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isSessionValid(String userId, String tokenId) {
        SessionEntry entry = sessionStore.get(tokenId);
        return entry != null && !entry.isExpired();
    }

    @Override
    public void cleanupExpiredSessions(String userId) {
        List<SessionDeviceResponse> sessions = getUserSessions(userId);
        LocalDateTime now = LocalDateTime.now();

        for (SessionDeviceResponse session : sessions) {
            if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
                removeSession(userId, session.getSessionId());
                logger.debug("清理过期会话: userId={}, sessionId={}", userId, session.getSessionId());
            }
        }
    }
}
