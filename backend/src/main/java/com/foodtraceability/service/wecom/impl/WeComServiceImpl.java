package com.foodtraceability.service.wecom.impl;

import com.foodtraceability.common.Result;
import com.foodtraceability.config.wecom.WeComProperties;
import com.foodtraceability.service.wecom.WeComService;
import com.foodtraceability.service.wecom.WeComUserInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.util.*;

@Service
public class WeComServiceImpl implements WeComService {

    private static final Logger logger = LoggerFactory.getLogger(WeComServiceImpl.class);

    private static final String ACCESS_TOKEN_KEY = "wecom:access_token";
    private static final String JSAPI_TICKET_KEY = "wecom:jsapi_ticket";

    private static final long TOKEN_EXPIRE_SECONDS = 7000;
    private static final long TICKET_EXPIRE_SECONDS = 7000;

    private final WeComProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** access_token 本地缓存（带 TTL，替代 Redis） */
    private volatile TokenCacheEntry accessTokenCache;
    /** jsapi_ticket 本地缓存（带 TTL，替代 Redis） */
    private volatile TokenCacheEntry jsapiTicketCache;

    public WeComServiceImpl(WeComProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Token 缓存条目，存储值和过期时间（替代 Redis TTL）
     */
    private static final class TokenCacheEntry {
        final String value;
        final long expireAt;

        TokenCacheEntry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    @Override
    public Result<Map<String, Object>> getJsApiConfig(String url) {
        try {
            String jsapiTicket = getJsApiTicket();
            String nonceStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
            long timestamp = System.currentTimeMillis() / 1000;
            String signature = sign(jsapiTicket, nonceStr, timestamp, url);

            Map<String, Object> config = new LinkedHashMap<>();
            config.put("corpId", properties.getCorpId());
            config.put("agentId", properties.getAgent().getAgentId());
            config.put("timestamp", timestamp);
            config.put("nonceStr", nonceStr);
            config.put("signature", signature);

            return Result.success(config);
        } catch (Exception e) {
            logger.error("获取企业微信JSAPI配置失败", e);
            return Result.error("获取JSAPI配置失败");
        }
    }

    @Override
    public Result<WeComUserInfo> getUserByCode(String code) {
        try {
            String accessToken = getAccessToken();
            String getUrl = String.format(
                "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?access_token=%s&code=%s",
                accessToken, code
            );

            ResponseEntity<String> response = restTemplate.exchange(getUrl, HttpMethod.GET, null, String.class);
            JsonNode body = objectMapper.readTree(response.getBody());

            int errcode = body.has("errcode") ? body.get("errcode").asInt() : 0;
            if (errcode != 0) {
                logger.warn("获取企业微信用户信息失败: errcode={}, errmsg={}", errcode, body.path("errmsg").asText(""));
                return Result.error("企业微信授权失败: " + body.path("errmsg").asText(""));
            }

            String userId = body.get("UserId").asText();

            String userDetailUrl = String.format(
                "https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=%s&userid=%s",
                accessToken, userId
            );
            ResponseEntity<String> detailResponse = restTemplate.exchange(userDetailUrl, HttpMethod.GET, null, String.class);
            JsonNode detailBody = objectMapper.readTree(detailResponse.getBody());

            WeComUserInfoImpl userInfo = new WeComUserInfoImpl();
            userInfo.setUserId(userId);
            userInfo.setName(detailBody.path("name").asText(""));
            userInfo.setMobile(detailBody.path("mobile").asText(""));
            userInfo.setAvatar(detailBody.path("avatar").asText(""));
            userInfo.setPosition(detailBody.path("position").asText(""));

            JsonNode department = detailBody.path("department");
            if (department.isArray() && department.size() > 0) {
                userInfo.setDepartment(department.get(0).asText(""));
            }

            return Result.success(userInfo);
        } catch (Exception e) {
            logger.error("企业微信用户登录异常", e);
            return Result.error("企业微信登录失败");
        }
    }

    @Override
    public Result<Void> sendTextMessage(String userId, String content) {
        try {
            String accessToken = getAccessToken();
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("touser", userId);
            msg.put("msgtype", "text");
            msg.put("agentid", Integer.parseInt(properties.getAgent().getAgentId()));
            Map<String, String> textContent = new HashMap<>();
            textContent.put("content", content);
            msg.put("text", textContent);

            String url = String.format(
                "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s",
                accessToken
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(msg), headers);
            restTemplate.postForEntity(url, entity, String.class);

            return Result.success(null);
        } catch (Exception e) {
            logger.error("发送企业微信消息失败", e);
            return Result.error("发送消息失败");
        }
    }

    @Override
    public Result<Void> sendTemplateCard(String userId, String cardJson) {
        try {
            String accessToken = getAccessToken();
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("touser", userId);
            msg.put("msgtype", "template_card");
            msg.put("agentid", Integer.parseInt(properties.getAgent().getAgentId()));
            Map<String, String> cardContent = new HashMap<>();
            cardContent.put("template_card", cardJson);
            msg.put("template_card", objectMapper.readTree(cardJson));

            String url = String.format(
                "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s",
                accessToken
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(msg), headers);
            restTemplate.postForEntity(url, entity, String.class);

            return Result.success(null);
        } catch (Exception e) {
            logger.error("发送模板卡片消息失败", e);
            return Result.error("发送消息失败");
        }
    }

    @Override
    public boolean isInWeComEnv(String userAgent) {
        if (userAgent == null) return false;
        return userAgent.contains("wxwork") || userAgent.contains("MicroMessenger");
    }

    private String getAccessToken() {
        // 检查本地缓存（替代 Redis）
        TokenCacheEntry cached = accessTokenCache;
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }

        String url = String.format(
            "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s",
            properties.getCorpId(), properties.getAgent().getSecret()
        );

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JsonNode body;
        try {
            body = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析企业微信access_token响应失败", e);
        }

        int errcode = body.has("errcode") ? body.get("errcode").asInt() : 0;
        if (errcode != 0) {
            throw new RuntimeException("获取access_token失败: " + body.path("errmsg").asText(""));
        }

        String token = body.get("access_token").asText();
        accessTokenCache = new TokenCacheEntry(token, System.currentTimeMillis() + TOKEN_EXPIRE_SECONDS * 1000);
        return token;
    }

    private String getJsApiTicket() {
        // 检查本地缓存（替代 Redis）
        TokenCacheEntry cached = jsapiTicketCache;
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }

        String accessToken = getAccessToken();
        String url = String.format(
            "https://qyapi.weixin.qq.com/cgi-bin/ticket/get?access_token=%s&type=agent_config",
            accessToken
        );

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JsonNode body;
        try {
            body = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析企业微信jsapi_ticket响应失败", e);
        }

        String ticket = body.get("ticket").asText();
        jsapiTicketCache = new TokenCacheEntry(ticket, System.currentTimeMillis() + TICKET_EXPIRE_SECONDS * 1000);
        return ticket;
    }

    private String sign(String ticket, String nonceStr, long timestamp, String url) {
        try {
            String str = String.format("jsapi_ticket=%s&noncestr=%s&timestamp=%d&url=%s",
                ticket, nonceStr, timestamp, url);
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }

    static class WeComUserInfoImpl implements WeComUserInfo {
        private String userId;
        private String name;
        private String mobile;
        private String department;
        private String avatar;
        private String position;

        @Override public String getUserId() { return userId; }
        @Override public void setUserId(String v) { this.userId = v; }
        @Override public String getName() { return name; }
        @Override public void setName(String v) { this.name = v; }
        @Override public String getMobile() { return mobile; }
        @Override public void setMobile(String v) { this.mobile = v; }
        @Override public String getDepartment() { return department; }
        @Override public void setDepartment(String v) { this.department = v; }
        @Override public String getAvatar() { return avatar; }
        @Override public void setAvatar(String v) { this.avatar = v; }
        @Override public String getPosition() { return position; }
        @Override public void setPosition(String v) { this.position = v; }
    }
}
