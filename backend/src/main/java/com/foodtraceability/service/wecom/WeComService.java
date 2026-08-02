package com.foodtraceability.service.wecom;

import com.foodtraceability.common.Result;
import com.foodtraceability.config.wecom.WeComProperties;

import java.util.Map;

public interface WeComService {

    Result<Map<String, Object>> getJsApiConfig(String url);

    Result<WeComUserInfo> getUserByCode(String code);

    Result<Void> sendTextMessage(String userId, String content);

    Result<Void> sendTemplateCard(String userId, String cardJson);

    boolean isInWeComEnv(String userAgent);
}
