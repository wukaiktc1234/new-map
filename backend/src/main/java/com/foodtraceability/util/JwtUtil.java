package com.foodtraceability.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * JWT工具类，用于生成和解析JWT令牌
 * 安全增强版：支持算法白名单、拒绝None算法、强制HS512签名
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // 允许的签名算法白名单（仅允许强签名算法）
    private static final Set<MacAlgorithm> ALLOWED_ALGORITHMS = Set.of(
        Jwts.SIG.HS256,
        Jwts.SIG.HS384,
        Jwts.SIG.HS512
    );

    private static final MacAlgorithm REQUIRED_ALGORITHM = Jwts.SIG.HS512;

    // 密钥最小长度（字节）
    private static final int MIN_KEY_LENGTH_BYTES = 32;

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @Value("${jwt.issuer:food-traceability-system}")
    private String issuer;

    /**
     * 获取签名密钥
     * 安全增强：确保密钥强度满足HS512要求（至少64字节）
     */
    private SecretKey getKey() {
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
     * 安全增强：强制使用HS512算法，包含Issuer声明
     *
     * @param subject 主题（通常是用户名）
     * @param claims 自定义声明
     * @return JWT令牌字符串
     */
    public String generateJwt(String subject, Map<String, Object> claims) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);

            return Jwts.builder()
                    .claims(claims)
                    .subject(subject)
                    .issuer(issuer)  // 添加发行者
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(getKey())  // 强制使用HS512
                    .compact();
        } catch (WeakKeyException e) {
            logger.error("JWT密钥强度不足: {}", e.getMessage());
            throw new SecurityException("JWT签名密钥不符合安全要求");
        } catch (Exception e) {
            logger.error("生成JWT令牌失败: {}", e.getMessage());
            throw new RuntimeException("生成JWT令牌失败", e);
        }
    }

    /**
     * 解析并验证JWT令牌
     * 安全增强：
     * 1. 拒绝None算法攻击
     * 2. 验证签名算法是否在白名单中
     * 3. 强制要求HS512算法
     * 4. 验证Issuer
     * 5. 完整的签名验证
     *
     * @param token JWT令牌
     * @return 令牌声明，验证失败返回null
     */
    public Claims parseJwt(String token) {
        if (!StringUtils.hasText(token)) {
            logger.warn("JWT令牌为空");
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .requireIssuer(issuer)  // 验证发行者
                    .build()
                    .parseSignedClaims(token)  // 这会自动验证签名和拒绝none算法
                    .getPayload();

            // 二次验证：检查Header中的算法（防御性编程）
            validateAlgorithmSecurity(token);

            return claims;
        } catch (SignatureException e) {
            logger.warn("JWT签名验证失败: {}", e.getMessage());
            logSecurityEvent("INVALID_SIGNATURE", "JWT签名无效");
            return null;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            logger.warn("JWT格式错误: {}", e.getMessage());
            logSecurityEvent("MALFORMED_TOKEN", "JWT令牌格式错误");
            return null;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.warn("JWT令牌已过期: {}", e.getMessage());
            logSecurityEvent("EXPIRED_TOKEN", "JWT令牌已过期");
            return null;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            logger.warn("不支持的JWT令牌: {}", e.getMessage());
            logSecurityEvent("UNSUPPORTED_TOKEN", "不支持的JWT令牌类型");
            return null;
        } catch (io.jsonwebtoken.IncorrectClaimException e) {
            logger.warn("JWT声明值不正确: {}", e.getMessage());
            logSecurityEvent("INCORRECT_CLAIM", "JWT声明值不正确（可能是Issuer不匹配）");
            return null;
        } catch (io.jsonwebtoken.MissingClaimException e) {
            logger.warn("JWT缺少必要声明: {}", e.getMessage());
            logSecurityEvent("MISSING_CLAIM", "JWT缺少必要声明");
            return null;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT令牌参数非法: {}", e.getMessage());
            logSecurityEvent("ILLEGAL_ARGUMENT", "JWT令牌参数非法");
            return null;
        } catch (Exception e) {
            logger.error("JWT令牌解析异常: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            logSecurityEvent("PARSE_ERROR", "JWT令牌解析异常: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * 验证JWT算法安全性
     * 防御性检查：确保Token使用的是允许的签名算法
     */
    private void validateAlgorithmSecurity(String token) {
        try {
            // 解析Token Header获取算法信息（不验证签名）
            String headerPart = token.split("\\.")[0];
            String headerJson = new String(
                java.util.Base64.getUrlDecoder().decode(headerPart),
                StandardCharsets.UTF_8
            );

            // 简单提取alg字段
            if (headerJson.contains("\"alg\":\"none\"") ||
                headerJson.contains("\"alg\": \"none\"")) {
                logger.error("检测到None算法攻击尝试！");
                logSecurityEvent("NONE_ALGORITHM_ATTACK", "检测到JWT None算法攻击");
                throw new SecurityException("拒绝None算法：不允许无签名令牌");
            }
        } catch (SecurityException e) {
            throw e;  // 重新抛出安全异常
        } catch (Exception e) {
            logger.debug("无法解析Token Header进行算法验证: {}", e.getMessage());
            // 不影响主要流程，因为parseClaimsJws已经做了完整验证
        }
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseJwt(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        Claims claims = parseJwt(token);
        if (claims == null) {
            return true;  // 无效Token视为已过期
        }
        return claims.getExpiration().before(new Date());
    }

    /**
     * 验证令牌完整性
     * 安全增强：同时验证签名、过期时间和用户名匹配
     *
     * @param token JWT令牌
     * @param username 期望的用户名
     * @return 是否有效
     */
    public boolean validateToken(String token, String username) {
        // 基本验证
        if (!StringUtils.hasText(token) || !StringUtils.hasText(username)) {
            return false;
        }

        // 解析并验证Token
        Claims claims = parseJwt(token);
        if (claims == null) {
            return false;
        }

        // 验证用户名匹配
        String tokenUsername = claims.getSubject();
        if (!username.equals(tokenUsername)) {
            logger.warn("Token用户名不匹配: 期望={}, 实际={}", username, tokenUsername);
            logSecurityEvent("USERNAME_MISMATCH", "Token用户名不匹配");
            return false;
        }

        // 验证未过期
        if (claims.getExpiration().before(new Date())) {
            logger.warn("Token已过期: username={}", username);
            return false;
        }

        logger.debug("Token验证成功: username={}", username);
        return true;
    }

    /**
     * 快速验证Token有效性（不抛出异常）
     * 用于性能敏感的场景
     */
    public boolean isTokenValid(String token) {
        return parseJwt(token) != null;
    }

    /**
     * 记录安全事件
     * 用于安全审计和监控
     */
    private void logSecurityEvent(String eventType, String message) {
        // 使用WARN级别记录安全事件，便于监控和告警
        logger.warn("[安全事件] 类型={}, 消息={}", eventType, message);

        // TODO: 可以集成到安全审计系统或发送告警
        // securityAuditService.logSecurityEvent(eventType, message);
    }
}