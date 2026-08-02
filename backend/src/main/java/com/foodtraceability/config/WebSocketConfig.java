package com.foodtraceability.config;

import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * WebSocket配置类
 * 配置WebSocket服务器端点、消息代理和STOMP认证拦截器
 * <p>
 * VULN-09 修复：
 * 1. 注入 JwtUtils 替代伪造的 validateToken，进行真实 JWT 校验
 * 2. CONNECT 命令认证失败时拒绝连接（返回 null），不再放行未认证用户
 * 3. SecurityConfig 中 /ws/** permitAll 保留（SockJS 握手兼容），
 *    实际鉴权完全在 STOMP CONNECT 帧完成
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtUtils jwtUtils;

    public WebSocketConfig(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        logger.warn("WebSocket连接被拒绝: 缺少Authorization header");
                        return null;
                    }
                    String token = authHeader.substring(7);
                    try {
                        Authentication auth = validateToken(token);
                        if (auth == null) {
                            logger.warn("WebSocket连接被拒绝: 无效或过期token");
                            return null;
                        }
                        accessor.setUser(auth);
                        logger.debug("WebSocket认证成功: user={}", auth.getName());
                    } catch (Exception e) {
                        logger.warn("WebSocket认证异常: {}", e.getMessage());
                        return null;
                    }
                }

                return message;
            }
        });
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(128 * 1024);
        registry.setSendBufferSizeLimit(512 * 1024);
        registry.setSendTimeLimit(20 * 1000);
    }

    /**
     * 真实 JWT 校验：调用 JwtUtils 验证签名/过期/黑名单，并提取用户身份与角色
     * @return 认证对象；token 无效或获取用户信息失败时返回 null
     */
    private Authentication validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (!jwtUtils.validateToken(token)) {
            return null;
        }
        Optional<SecurityUser> userOpt = jwtUtils.getUserFromToken(token);
        if (userOpt.isEmpty()) {
            return null;
        }
        SecurityUser user = userOpt.get();
        List<String> roles = user.getRoles();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            for (String role : roles) {
                String normalized = role.toUpperCase().startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
                authorities.add(new SimpleGrantedAuthority(normalized));
            }
        }
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
    }
}
