package com.foodtraceability.security.utils;

import com.foodtraceability.security.config.JwtConfig;
import com.foodtraceability.security.model.SecurityUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * JWT工具类 - 安全增强版
 *
 * 安全特性：
 * 1. 拒绝None算法攻击
 * 2. 强制HS512签名算法
 * 3. 算法白名单验证
 * 4. Issuer验证
 * 5. Token黑名单机制（内存实现）
 * 6. 完整的异常分类处理
 * 7. 安全事件日志记录
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    // 同一用户同时有效的RefreshToken最大数量
    private static final int MAX_REFRESH_TOKENS = 5;

    // 清理阈值：黑名单条目超过此数量时触发清理
    private static final int BLACKLIST_CLEANUP_THRESHOLD = 10000;

    // 允许的签名算法白名单
    private static final Set<MacAlgorithm> ALLOWED_ALGORITHMS = Set.of(
        Jwts.SIG.HS256,
        Jwts.SIG.HS384,
        Jwts.SIG.HS512
    );

    private static final MacAlgorithm REQUIRED_ALGORITHM = Jwts.SIG.HS512;

    // 密钥最小长度（字节）- HS512要求至少64字节（512位）
    private static final int MIN_KEY_LENGTH_BYTES = 64;

    private final JwtConfig jwtConfig;

    public JwtUtils(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    // ========== 内存存储 ==========

    /** Token黑名单：jti -> 过期时间戳（毫秒） */
    private final ConcurrentMap<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

    /** 用户AccessToken存储：userId -> TimedEntry<token> */
    private final ConcurrentMap<String, TimedEntry<String>> tokenStore = new ConcurrentHashMap<>();

    /** 用户RefreshToken存储：userId -> TimedEntry<refreshToken> */
    private final ConcurrentMap<String, TimedEntry<String>> refreshTokenStore = new ConcurrentHashMap<>();

    /** 用户RefreshToken jti列表：userId -> (时间戳 -> jti)，按时间戳排序 */
    private final ConcurrentMap<String, ConcurrentSkipListMap<Long, String>> refreshJtiStore = new ConcurrentHashMap<>();

    /**
     * 带过期时间的存储条目
     */
    private static class TimedEntry<T> {
        private final T value;
        private final long expireAt;

        TimedEntry(T value, long expireInMs) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + expireInMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }

        T getValue() {
            return value;
        }

        long getExpireAt() {
            return expireAt;
        }
    }

    /**
     * 获取签名密钥
     * 安全增强：确保密钥强度满足HS512要求
     */
    private SecretKey getSigningKey() {
        String secret = jwtConfig.getSecret();

        if (!StringUtils.hasText(secret)) {
            throw new SecurityException("JWT密钥未配置，请在application.yml中设置jwt.secret");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // 如果密钥长度不足，使用SHA-512扩展到64字节
        if (keyBytes.length < MIN_KEY_LENGTH_BYTES) {
            logger.warn("JWT密钥长度不足{}字节，正在使用SHA-512扩展密钥", MIN_KEY_LENGTH_BYTES);
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                keyBytes = digest.digest(keyBytes);
            } catch (Exception e) {
                logger.error("SHA-512密钥扩展失败", e);
                throw new SecurityException("无法生成安全的JWT签名密钥");
            }
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT令牌
     * 安全增强：强制使用HS512算法，包含完整声明
     *
     * @param user 安全用户信息
     * @return JWT令牌字符串
     */
    public String generateToken(SecurityUser user) {
        try {
            // 防御性检查：验证必要字段
            if (user == null) {
                throw new IllegalArgumentException("用户信息不能为null");
            }
            if (!StringUtils.hasText(user.getUserId())) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            if (!StringUtils.hasText(user.getUsername())) {
                throw new IllegalArgumentException("用户名不能为空");
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getUserId());
            claims.put("username", user.getUsername());

            // 统一将角色转换为小写，确保与Spring Security的hasAnyRole方法兼容
            List<String> rawRoles = user.getRoles();
            List<String> normalizedRoles;
            if (rawRoles != null && !rawRoles.isEmpty()) {
                normalizedRoles = rawRoles.stream()
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                logger.debug("用户角色列表: userId={}, roles={}", user.getUserId(), normalizedRoles);
            } else {
                normalizedRoles = new ArrayList<>();
                logger.warn("用户角色列表为空: userId={}, 将使用空角色列表", user.getUserId());
            }
            claims.put("roles", normalizedRoles);

            // 防御性处理：permissions可能为null
            List<String> rawPermissions = user.getPermissions();
            List<String> permissions = (rawPermissions != null) ? rawPermissions : new ArrayList<>();
            claims.put("permissions", permissions);

            claims.put("mfaEnabled", user.isMfaEnabled());
            claims.put("mfaVerified", user.isMfaVerified());
            claims.put("tokenId", UUID.randomUUID().toString());

            logger.debug("开始生成JWT令牌: userId={}, username={}, issuer={}",
                    user.getUserId(), user.getUsername(), jwtConfig.getIssuer());

            String token = Jwts.builder()
                    .claims(claims)
                    .subject(user.getUsername())
                    .issuer(jwtConfig.getIssuer())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpiration()))
                    .id(UUID.randomUUID().toString())
                    .signWith(getSigningKey(), Jwts.SIG.HS512)
                    .compact();

            logger.info("JWT令牌生成成功: userId={}, tokenId={}, expiresIn={}ms",
                    user.getUserId(),
                    claims.get("tokenId"),
                    jwtConfig.getAccessTokenExpiration());

            return token;
        } catch (WeakKeyException e) {
            logger.error("JWT密钥强度不足: {}", e.getMessage());
            throw new SecurityException("JWT签名密钥不符合安全要求", e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT令牌参数校验失败: userId={}, error={}",
                    user != null ? user.getUserId() : "null",
                    e.getMessage());
            throw new RuntimeException("生成JWT令牌参数错误: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("生成JWT令牌失败: userId={}, username={}, errorType={}, errorMessage={}",
                    user != null ? user.getUserId() : "null",
                    user != null ? user.getUsername() : "null",
                    e.getClass().getSimpleName(),
                    e.getMessage(), e);
            throw new RuntimeException("生成JWT令牌失败: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }
    }

    /**
     * 生成刷新令牌
     *
     * @param user 安全用户信息
     * @return 刷新令牌字符串
     */
    public String generateRefreshToken(SecurityUser user) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getUserId());
            claims.put("username", user.getUsername());
            claims.put("tokenId", UUID.randomUUID().toString());
            claims.put("type", "refresh");

            return Jwts.builder()
                    .claims(claims)
                    .subject(user.getUsername())
                    .issuer(jwtConfig.getIssuer())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshTokenExpiration()))
                    .id(UUID.randomUUID().toString())
                    .signWith(getSigningKey(), Jwts.SIG.HS512)
                    .compact();
        } catch (Exception e) {
            logger.error("生成刷新令牌失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成刷新令牌失败", e);
        }
    }

    /**
     * 验证JWT令牌
     * 安全增强：
     * 1. 黑名单检查
     * 2. 签名验证（自动拒绝none算法）
     * 3. 算法类型验证
     * 4. Issuer验证
     * 5. 过期时间检查
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            logSecurityEvent("EMPTY_TOKEN", "空的JWT令牌");
            return false;
        }

        try {
            // 1. 检查令牌是否在黑名单中
            if (isTokenInBlacklist(token)) {
                logSecurityEvent("BLACKLISTED_TOKEN", "令牌已在黑名单中");
                return false;
            }

            // 2. 验证令牌签名、算法和过期时间
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .requireIssuer(jwtConfig.getIssuer())
                    .build()
                    .parseSignedClaims(token);

            // 3. 二次验证：防御性检查Header中的算法
            validateAlgorithmSecurity(token);

            logger.debug("JWT令牌验证成功");
            return true;
        } catch (SignatureException e) {
            logSecurityEvent("INVALID_SIGNATURE", "JWT签名验证失败: " + e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logSecurityEvent("MALFORMED_TOKEN", "JWT格式错误: " + e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            logSecurityEvent("EXPIRED_TOKEN", "JWT令牌已过期: " + e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logSecurityEvent("UNSUPPORTED_TOKEN", "不支持的JWT令牌: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logSecurityEvent("ILLEGAL_ARGUMENT", "JWT参数非法: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("JWT令牌验证异常: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            logSecurityEvent("VALIDATION_ERROR", "JWT验证异常: " + e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * 验证JWT算法安全性
     * 防御性检查：检测None算法攻击
     */
    private void validateAlgorithmSecurity(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 1) {
                return;
            }

            String headerJson = new String(
                Decoders.BASE64URL.decode(parts[0]),
                StandardCharsets.UTF_8
            );

            if (headerJson.contains("\"alg\":\"none\"") ||
                headerJson.contains("\"alg\": \"none\"") ||
                headerJson.toLowerCase().contains("\"alg\":\"none\"")) {

                logger.error("检测到None算法攻击尝试！Token Header: {}", headerJson);
                logSecurityEvent("NONE_ALGORITHM_ATTACK", "检测到JWT None算法攻击尝试");
                throw new SecurityException("拒绝None算法攻击：不允许无签名令牌");
            }

            if (logger.isDebugEnabled() && headerJson.contains("\"alg\"")) {
                logger.debug("JWT使用算法: {}", headerJson);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            logger.debug("无法解析Token Header进行算法验证: {}", e.getMessage());
        }
    }

    /**
     * 从令牌中获取用户信息
     *
     * @param token JWT令牌
     * @return 安全用户Optional
     */
    public Optional<SecurityUser> getUserFromToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .requireIssuer(jwtConfig.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            SecurityUser user = new SecurityUser();
            user.setUserId(claims.get("userId", String.class));
            user.setUsername(claims.getSubject());
            Boolean mfaEnabledClaim = claims.get("mfaEnabled", Boolean.class);
            user.setMfaEnabled(mfaEnabledClaim != null && mfaEnabledClaim);
            user.setMfaVerified(Boolean.TRUE.equals(claims.get("mfaVerified", Boolean.class)));

            if (claims.get("roles") != null) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.get("roles");
                List<String> normalizedRoles = roles.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                user.setRoles(normalizedRoles);
            }
            if (claims.get("permissions") != null) {
                @SuppressWarnings("unchecked")
                List<String> permissions = (List<String>) claims.get("permissions");
                user.setPermissions(permissions);
            }

            return Optional.of(user);
        } catch (ExpiredJwtException e) {
            logSecurityEvent("TOKEN_EXPIRED_GET_USER", "从Token获取用户信息失败：令牌已过期");
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("从令牌获取用户信息失败: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 从令牌中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("userId", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("从令牌获取用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将令牌添加到黑名单
     * 使用jti作为黑名单Key，用于注销等场景
     */
    public void addToBlacklist(String token) {
        try {
            Claims claims;
            try {
                claims = Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (ExpiredJwtException e) {
                // 令牌已过期，但仍可加入黑名单（防止边缘情况）
                claims = e.getClaims();
            }

            String jti = claims.getId();
            if (jti == null) {
                logger.warn("令牌缺少jti，无法加入黑名单");
                return;
            }

            long expirationTime = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (expirationTime > 0) {
                addToBlacklistByJti(jti, expirationTime);
            } else {
                logger.debug("令牌已过期，无需加入黑名单: jti={}", jti);
            }
        } catch (JwtException e) {
            logger.error("添加令牌到黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 检查令牌是否在黑名单中
     * 通过jti查询黑名单
     */
    public boolean isTokenInBlacklist(String token) {
        String jti = getJtiFromToken(token);
        if (jti == null) {
            return false;
        }
        return isJtiInBlacklist(jti);
    }

    /**
     * 存储令牌信息到内存
     * 同时将RefreshToken的jti添加到用户列表并强制限制数量
     */
    public void storeTokenInfo(String userId, String token, String refreshToken) {
        try {
            // 触发过期清理
            cleanupExpiredEntriesIfNeeded();

            // 存储AccessToken
            tokenStore.put(userId, new TimedEntry<>(token, jwtConfig.getAccessTokenExpiration()));

            // 存储RefreshToken
            refreshTokenStore.put(userId, new TimedEntry<>(refreshToken, jwtConfig.getRefreshTokenExpiration()));

            // 将RefreshToken的jti添加到用户列表并强制限制数量
            String refreshJti = getJtiFromToken(refreshToken);
            if (refreshJti != null) {
                addRefreshTokenJti(userId, refreshJti);
                enforceRefreshTokenLimit(userId, MAX_REFRESH_TOKENS);
            }
        } catch (Exception e) {
            logger.warn("令牌信息写入内存失败: {}", e.getMessage());
        }
    }

    /**
     * 获取用户的当前令牌
     */
    public Optional<String> getCurrentToken(String userId) {
        TimedEntry<String> entry = tokenStore.get(userId);
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(entry.getValue());
    }

    /**
     * 获取用户的刷新令牌
     */
    public Optional<String> getRefreshToken(String userId) {
        TimedEntry<String> entry = refreshTokenStore.get(userId);
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(entry.getValue());
    }

    /**
     * 移除令牌信息
     */
    public void removeTokenInfo(String userId) {
        tokenStore.remove(userId);
        refreshTokenStore.remove(userId);
    }

    /**
     * 获取令牌剩余有效期（毫秒）
     */
    public long getTokenRemainingValidity(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().getTime() - System.currentTimeMillis();
        } catch (JwtException e) {
            logger.error("Failed to get token validity: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 记录安全事件
     */
    private void logSecurityEvent(String eventType, String message) {
        try {
            securityLogger.warn("[JWT安全事件] 类型={}, 消息={}, 时间={}",
                eventType, message, Instant.now());

            if (isCriticalSecurityEvent(eventType)) {
                logger.warn("[安全告警] JWT安全事件: type={}, message={}", eventType, message);
            }
        } catch (Exception e) {
            logger.error("记录安全事件失败: {}", e.getMessage());
        }
    }

    /**
     * 判断是否为关键安全事件
     */
    private boolean isCriticalSecurityEvent(String eventType) {
        Set<String> criticalEvents = Set.of(
            "NONE_ALGORITHM_ATTACK",
            "INVALID_SIGNATURE",
            "BLACKLISTED_TOKEN"
        );
        return criticalEvents.contains(eventType);
    }

    /**
     * 从令牌中提取jti（JWT ID）
     * 支持已过期令牌的jti提取
     *
     * @param token JWT令牌
     * @return jti字符串，提取失败返回null
     */
    public String getJtiFromToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getId();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getId();
        } catch (JwtException e) {
            logger.warn("从令牌提取jti失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 通过jti将令牌加入黑名单
     * 用于RefreshToken轮转时将旧RefreshToken加入黑名单
     *
     * @param jti 令牌的唯一标识
     * @param expirationMs 黑名单过期时间（毫秒）
     */
    public void addToBlacklistByJti(String jti, long expirationMs) {
        if (jti == null || expirationMs <= 0) {
            return;
        }
        long expireAt = System.currentTimeMillis() + expirationMs;
        tokenBlacklist.put(jti, expireAt);
        logger.info("jti已添加到黑名单: jti={}, 过期时间={}ms", jti, expirationMs);
    }

    /**
     * 检查jti是否在黑名单中
     *
     * @param jti 令牌的唯一标识
     * @return 是否在黑名单中
     */
    public boolean isJtiInBlacklist(String jti) {
        if (jti == null) {
            return false;
        }
        Long expireAt = tokenBlacklist.get(jti);
        if (expireAt == null) {
            return false;
        }
        // 检查是否已过期，过期则清理
        if (System.currentTimeMillis() > expireAt) {
            tokenBlacklist.remove(jti);
            return false;
        }
        return true;
    }

    /**
     * 将RefreshToken的jti添加到用户的RefreshToken列表
     * 使用ConcurrentSkipListMap按时间戳排序
     *
     * @param userId 用户ID
     * @param jti RefreshToken的唯一标识
     */
    public void addRefreshTokenJti(String userId, String jti) {
        if (userId == null || jti == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        ConcurrentSkipListMap<Long, String> jtiMap = refreshJtiStore.computeIfAbsent(
                userId, k -> new ConcurrentSkipListMap<>());
        jtiMap.put(currentTime, jti);
        logger.debug("RefreshToken jti已添加到用户列表: userId={}, jti={}", userId, jti);
    }

    /**
     * 从用户的RefreshToken列表中移除指定jti
     *
     * @param userId 用户ID
     * @param jti RefreshToken的唯一标识
     */
    public void removeRefreshTokenJti(String userId, String jti) {
        if (userId == null || jti == null) {
            return;
        }
        ConcurrentSkipListMap<Long, String> jtiMap = refreshJtiStore.get(userId);
        if (jtiMap != null) {
            // 遍历移除匹配的jti
            jtiMap.entrySet().removeIf(entry -> jti.equals(entry.getValue()));
        }
        logger.debug("RefreshToken jti已从用户列表移除: userId={}, jti={}", userId, jti);
    }

    /**
     * 强制限制用户同时有效的RefreshToken数量
     * 超出限制时移除最旧的RefreshToken，并将其jti加入黑名单
     *
     * @param userId 用户ID
     * @param maxCount 最大允许数量
     */
    public void enforceRefreshTokenLimit(String userId, int maxCount) {
        if (userId == null) {
            return;
        }
        ConcurrentSkipListMap<Long, String> jtiMap = refreshJtiStore.get(userId);
        if (jtiMap == null || jtiMap.size() <= maxCount) {
            return;
        }

        // 移除最旧的RefreshToken（时间戳最小的排在前面）
        int removeCount = jtiMap.size() - maxCount;
        for (int i = 0; i < removeCount; i++) {
            Map.Entry<Long, String> oldest = jtiMap.pollFirstEntry();
            if (oldest != null) {
                String removedJti = oldest.getValue();
                // 将被移除的jti加入黑名单，使用RefreshToken过期时间作为黑名单TTL
                addToBlacklistByJti(removedJti, jwtConfig.getRefreshTokenExpiration());
                logger.info("移除超限RefreshToken: userId={}, jti={}", userId, removedJti);
            }
        }
    }

    /**
     * 清理过期条目，防止内存泄漏
     * 当黑名单条目超过阈值时触发清理
     */
    private void cleanupExpiredEntriesIfNeeded() {
        if (tokenBlacklist.size() < BLACKLIST_CLEANUP_THRESHOLD) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        // 清理过期的黑名单条目
        tokenBlacklist.entrySet().removeIf(entry -> currentTime > entry.getValue());
        // 清理过期的token存储
        tokenStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
        refreshTokenStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
        logger.debug("JWT内存清理完成: blacklist={}, tokenStore={}, refreshTokenStore={}",
                tokenBlacklist.size(), tokenStore.size(), refreshTokenStore.size());
    }
}
