package com.foodtraceability.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger配置类
 * 用于配置Swagger文档的基本信息、安全认证、API分组等
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "食品溯源系统 API",
                version = "1.0.0",
                description = "食品溯源系统后端API文档，包含产品管理、仓储管理、订单管理等功能",
                contact = @Contact(name = "技术支持", email = "support@foodtraceability.com"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")
        ),
        servers = {
                @Server(url = "${API_SERVER_URL:http://localhost:8081}", description = "开发环境"),
                @Server(url = "${API_SERVER_URL_PROD:https://api.foodtraceability.com}", description = "生产环境")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenAPIConfig {

    /**
     * 产品管理API分组
     */
    @Bean
    public GroupedOpenApi productApi() {
        return GroupedOpenApi.builder()
                .group("产品管理")
                .pathsToMatch("/v1/products/**")
                .build();
    }

    /**
     * 仓储管理API分组
     */
    @Bean
    public GroupedOpenApi inventoryApi() {
        return GroupedOpenApi.builder()
                .group("仓储管理")
                .pathsToMatch("/v1/inventory/**")
                .build();
    }

    /**
     * 订单管理API分组
     */
    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
                .group("订单管理")
                .pathsToMatch("/v1/orders/**")
                .build();
    }

    /**
     * 认证管理API分组
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("认证管理")
                .pathsToMatch("/v1/auth/**")
                .build();
    }

    /**
     * 系统管理API分组
     */
    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("系统管理")
                .pathsToMatch("/v1/permissions/**", "/v1/roles/**", "/v1/users/**")
                .build();
    }

    /**
     * 财务报表API分组
     */
    @Bean
    public GroupedOpenApi financeApi() {
        return GroupedOpenApi.builder()
                .group("财务报表")
                .pathsToMatch("/v1/finance/**")
                .build();
    }

    /**
     * 溯源码管理API分组
     */
    @Bean
    public GroupedOpenApi traceCodeApi() {
        return GroupedOpenApi.builder()
                .group("溯源码管理")
                .pathsToMatch("/v1/trace-code/**")
                .build();
    }
}