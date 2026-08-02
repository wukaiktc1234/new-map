package com.foodtraceability.security.config;

import com.foodtraceability.security.filter.JwtAuthenticationFilter;
import com.foodtraceability.security.filter.MfaAuthenticationFilter;
import com.foodtraceability.security.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, MfaAuthenticationFilter mfaAuthenticationFilter, RateLimitFilter rateLimitFilter, UserDetailsService userDetailsService, LogoutHandler logoutHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.mfaAuthenticationFilter = mfaAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.userDetailsService = userDetailsService;
        this.logoutHandler = logoutHandler;
    }

    @Value("${app.cors.allowed-origins:https://yourdomain.com,https://www.yourdomain.com}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final MfaAuthenticationFilter mfaAuthenticationFilter;

    private final RateLimitFilter rateLimitFilter;

    private final UserDetailsService userDetailsService;

    private final LogoutHandler logoutHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                var config = new org.springframework.web.cors.CorsConfiguration();
                List<String> origins = java.util.Arrays.asList(allowedOrigins.split(","));
                config.setAllowedOriginPatterns(origins);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setExposedHeaders(List.of("Authorization"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);
                return config;
            }))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> {
                if (!securityEnabled) {
                    authz.anyRequest().permitAll();
                } else {
                    authz
                        // 公开端点 - 无需认证
                        // 注意：由于context-path设置为/api，服务器看到的路径不包含/api前缀
                        .requestMatchers("/v1/auth/**", "/auth/**").permitAll()
                        .requestMatchers("/v1/public/**", "/public/**").permitAll()
                        // 企业微信集成接口 - 公开访问（OAuth免登、JSAPI鉴权）
                        .requestMatchers("/v1/wecom/**").permitAll()
                        // 用户注册验证接口 - 公开访问（用于前端表单验证）
                        .requestMatchers("/v1/users/check-username").permitAll()
                        .requestMatchers("/v1/users/check-email").permitAll()
                        .requestMatchers("/v1/users/check-phone").permitAll()
                        .requestMatchers("/v1/test/**").permitAll()
                        .requestMatchers("/v1/ocr/**").permitAll()
                        .requestMatchers("/v1/epson-printer/**").permitAll()
                        .requestMatchers("/v1/label-printer/**").permitAll()
                        .requestMatchers("/v1/label-template/**").permitAll()
                        // 厨房操作 - GET查询公开, 写操作需认证
                        .requestMatchers(HttpMethod.GET, "/v1/kitchen/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/kitchen/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/v1/kitchen/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/v1/kitchen/**").authenticated()
                        // 厨房订单 - GET查询公开, 写操作需认证
                        .requestMatchers(HttpMethod.GET, "/v1/kitchen-order/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/kitchen-order/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/v1/kitchen-order/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/v1/kitchen-order/**").authenticated()
                        // 托盘管理 - GET公开, 写操作需认证
                        .requestMatchers(HttpMethod.GET, "/v1/tray/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/tray/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/v1/tray/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/v1/tray/**").authenticated()
                        // 资产管理 - GET公开, 写操作需认证
                        .requestMatchers(HttpMethod.GET, "/v1/asset/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/asset/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/v1/asset/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/v1/asset/**").authenticated()
                        // POS收银系统 - 认证接口公开，其他需认证(涉及资金交易)
                        .requestMatchers("/v1/pos/auth/**").permitAll()
                        .requestMatchers("/v1/pos/**").authenticated()
                        .requestMatchers("/pos/**").authenticated()
                        // 小程序公开API - 菜品查询
                        .requestMatchers("/v1/foods/**").permitAll()
                        .requestMatchers("/v1/food-categories/**").permitAll()
                        // 供应商API - 列表查询公开（用于下拉选择）
                        .requestMatchers(HttpMethod.GET, "/v1/suppliers/list").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/suppliers/statistics").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/suppliers/page").permitAll()
                        // 组织架构API - 部门树公开（用于采购计划等模块下拉选择）
                        .requestMatchers(HttpMethod.GET, "/v1/departments/tree").permitAll()
                        // 商品档案API - 需要认证
                        .requestMatchers("/v1/material-templates/**").authenticated()
                        // 采购申请API - 需要认证
                        .requestMatchers("/v1/purchase/**").authenticated()
                        // 电子凭证API - 需要认证
                        .requestMatchers("/v1/electronic-voucher/**").authenticated()
                        // 订单API - GET公开（查询状态），POST/PUT/DELETE需认证
                        .requestMatchers(HttpMethod.GET, "/v1/orders/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/orders").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/v1/orders/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/v1/orders/**").authenticated()
                        // 预约API - GET公开（查询状态），POST/PUT/DELETE需认证（与订单API保持一致）
                        .requestMatchers(HttpMethod.GET, "/v1/reservations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/reservations/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/v1/reservations/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/v1/reservations/**").authenticated()
                        .requestMatchers("/v1/mp/**").permitAll()
                        .requestMatchers("/mp/**").permitAll()
                        // WebSocket端点 - SockJS需要
                        .requestMatchers("/ws/**").permitAll()
                        // 静态资源 - 图片上传
                        .requestMatchers("/uploads/**").permitAll()
                        // 邀请码验证接口 - 注册时需要公开访问
                        .requestMatchers("/onboarding/invitation/validate").permitAll()
                        .requestMatchers("/onboarding/invitation/code/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Swagger/OpenAPI文档
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 静态资源
                        .requestMatchers("/", "/index.html", "/static/**", "/favicon.ico").permitAll()
                        // 所有其他请求需要认证
                        .anyRequest().authenticated();
                }
            })
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(mfaAuthenticationFilter, JwtAuthenticationFilter.class)
            .authenticationProvider(authenticationProvider())
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .addLogoutHandler(logoutHandler)
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                })
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"未认证，请先登录\"}");
                })
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:; frame-src 'self'; connect-src 'self' ws: wss: http: https:"))
                .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                .frameOptions(frameOptions -> frameOptions.deny())
            );
        
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
