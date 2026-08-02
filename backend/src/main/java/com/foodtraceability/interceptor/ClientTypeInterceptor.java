package com.foodtraceability.interceptor;

import com.foodtraceability.cache.ClientContextHolder;
import com.foodtraceability.cache.ClientType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ClientTypeInterceptor implements HandlerInterceptor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClientTypeInterceptor.class);
    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientTypeHeader = request.getHeader(CLIENT_TYPE_HEADER);
        ClientType clientType = ClientType.fromCode(clientTypeHeader);
        ClientContextHolder.setClientType(clientType);
        log.debug("请求路径: {}, 客户端类型: {}", request.getRequestURI(), clientType.getDescription());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ClientContextHolder.clear();
    }
}
