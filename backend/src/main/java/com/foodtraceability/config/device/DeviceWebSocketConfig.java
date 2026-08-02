package com.foodtraceability.config.device;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 设备WebSocket配置类
 * 配置WebSocket端点和消息代理
 */
@Configuration
@EnableWebSocketMessageBroker
public class DeviceWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 端点路径
     */
    private static final String ENDPOINT = "/ws/device";

    /**
     * 允许的来源
     * - 3000: 旧版前端端口（保留兼容）
     * - 3002: 当前前端开发端口
     * - 8081: 后端端口（同源调试）
     */
    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:3002",
            "http://localhost:8081"
    };

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的内存消息代理
        // 前端订阅 /topic/xxx 可以收到消息
        config.enableSimpleBroker("/topic");
        // 前端发送到 /app/xxx 会被@Controller处理
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket端点
        registry.addEndpoint(ENDPOINT)
                .setAllowedOrigins(ALLOWED_ORIGINS)
                .withSockJS(); // 支持SockJS降级
    }
}
