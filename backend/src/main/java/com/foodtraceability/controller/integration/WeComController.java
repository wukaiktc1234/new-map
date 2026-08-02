package com.foodtraceability.controller.integration;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.wecom.WeComService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "企业微信集成", description = "企业微信OAuth免登、JSAPI鉴权、消息推送等接口")
@RestController
@RequestMapping("/v1/wecom")
public class WeComController {

    private static final Logger logger = LoggerFactory.getLogger(WeComController.class);

    private final WeComService weComService;

    public WeComController(WeComService weComService) {
        this.weComService = weComService;
    }

    @Operation(summary = "检测是否在企业微信环境内")
    @GetMapping("/detect")
    public Result<Map<String, Object>> detect(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        boolean inWeCom = weComService.isInWeComEnv(userAgent);
        return Result.success(Map.of(
            "inWeCom", inWeCom,
            "userAgent", userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 120)) : ""
        ));
    }

    @Operation(summary = "获取JSAPI配置（wx.config鉴权参数）")
    @GetMapping("/jsapi-config")
    public Result<Map<String, Object>> getJsApiConfig(@RequestParam String url) {
        return weComService.getJsApiConfig(url);
    }

    @Operation(summary = "企业微信OAuth免登（用code换取用户信息）")
    @GetMapping("/oauth-login")
    public Result<Map<String, Object>> oauthLogin(@RequestParam String code) {
        var userResult = weComService.getUserByCode(code);
        if (userResult.getCode() != 0) {
            return Result.error("企微登录失败: " + (userResult.getMessage() != null ? userResult.getMessage() : "未知错误"));
        }
        var userInfo = userResult.getData();
        return Result.success(Map.of(
            "userId", userInfo.getUserId(),
            "name", userInfo.getName(),
            "mobile", userInfo.getMobile(),
            "avatar", userInfo.getAvatar(),
            "department", userInfo.getDepartment(),
            "position", userInfo.getPosition()
        ));
    }

    @Operation(summary = "发送文本消息给员工")
    @PostMapping("/message/send-text")
    public Result<Void> sendTextMessage(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String content = body.get("content");
        if (userId == null || userId.isEmpty() || content == null || content.isEmpty()) {
            return Result.error("参数不完整");
        }
        return weComService.sendTextMessage(userId, content);
    }

    @Operation(summary = "发送模板卡片消息")
    @PostMapping("/message/send-card")
    public Result<Void> sendTemplateCard(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String cardJson = body.get("cardJson");
        if (userId == null || userId.isEmpty() || cardJson == null || cardJson.isEmpty()) {
            return Result.error("参数不完整");
        }
        return weComService.sendTemplateCard(userId, cardJson);
    }

    @Operation(summary = "获取企业微信授权URL（用于网页授权跳转）")
    @GetMapping("/auth-url")
    public Result<String> getAuthUrl(@RequestParam String redirectUri) {
        String corpId = System.getProperty("wecom.corpId", "");
        String agentId = System.getProperty("wecom.agent.agentId", "");
        if (corpId.isEmpty()) {
            return Result.error("企业微信未配置");
        }
        String url = String.format(
            "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&agentid=%s#wechat_redirect",
            corpId, redirectUri, agentId
        );
        return Result.success(url);
    }
}
