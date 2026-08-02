package com.foodtraceability.controller.h5;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.MiniProgramOpenIdRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 小程序 API 控制器
 *
 * <p>注意：微信小程序登录接口（getOpenId）需要对接微信开放平台 API。</p>
 * <p>当前未配置微信小程序 AppID/AppSecret，接口返回明确错误。</p>
 * <p>TODO: 配置微信小程序参数后，调用 https://api.weixin.qq.com/sns/jscode2session 获取 openid</p>
 */
@RestController
@RequestMapping("/v1/mp")
@Tag(name = "小程序API", description = "小程序专用接口")
public class MiniProgramController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MiniProgramController.class);

    @PostMapping("/getOpenId")
    @Operation(summary = "获取用户openid", description = "通过微信 code 换取 openid（需配置微信小程序参数）")
    public Result<Map<String, String>> getOpenId(@RequestBody MiniProgramOpenIdRequest request) {
        log.warn("[小程序登录] 收到 getOpenId 请求, code: {}, 但微信小程序 API 未配置", request.getCode());
        return Result.error("微信小程序登录服务未配置，请联系管理员配置 AppID/AppSecret");
    }
}
