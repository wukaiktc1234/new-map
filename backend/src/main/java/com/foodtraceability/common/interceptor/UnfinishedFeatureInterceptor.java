package com.foodtraceability.common.interceptor;

import com.foodtraceability.common.Result;
import com.foodtraceability.common.annotation.UnfinishedFeature;
import com.foodtraceability.common.annotation.UnfinishedFeatures;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * P0-unfinished 门禁：命中 {@link UnfinishedFeature} 标注的入口时直接返回 501。
 *
 * <p>这些入口背后的表在开发库与生产库都不存在，调用必然撞 PSQLException → 500。
 * 500 表示系统故障，会污染告警与监控；501 语义明确（功能未上线）且可回滚。</p>
 */
public class UnfinishedFeatureInterceptor implements HandlerInterceptor {

    /** 功能未上线（项目暂无 501 业务码，用常量表达，不新增 ErrorCode 造码）。 */
    public static final int NOT_LAUNCHED = 501;

    public static final String MESSAGE = "该功能尚未上线（表结构未建立），请联系产品确认";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        boolean gated = hm.getMethod().isAnnotationPresent(UnfinishedFeature.class)
                || hm.getMethod().isAnnotationPresent(UnfinishedFeatures.class)
                || hm.getBeanType().isAnnotationPresent(UnfinishedFeature.class)
                || hm.getBeanType().isAnnotationPresent(UnfinishedFeatures.class);
        if (!gated) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        Result<Void> body = Result.error(NOT_LAUNCHED, MESSAGE);
        response.getWriter().write(MAPPER.writeValueAsString(body));
        return false;
    }
}
