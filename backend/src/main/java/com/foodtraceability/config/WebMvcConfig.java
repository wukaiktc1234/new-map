package com.foodtraceability.config;

import com.foodtraceability.common.interceptor.UnfinishedFeatureInterceptor;
import com.foodtraceability.interceptor.ClientTypeInterceptor;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @SuppressWarnings("nullness")

    public WebMvcConfig(ClientTypeInterceptor clientTypeInterceptor) {
        this.clientTypeInterceptor = clientTypeInterceptor;
    }

    /**
     * P0-unfinished 门禁：把 {@code @UnfinishedFeature} 标注的入口短路成 501。
     * 注册在最前面，避免被业务链或其他拦截器先执行后已经查询了不存在的表。
     */

    private final ClientTypeInterceptor clientTypeInterceptor;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(@Nonnull InterceptorRegistry registry) {
        registry.addInterceptor(new UnfinishedFeatureInterceptor()).addPathPatterns("/**").order(-100);
        registry.addInterceptor(clientTypeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/**", "/public/**", "/onboarding/invitation/**", "/uploads/**", "/api/uploads/**");
    }

    @Override
    public void addResourceHandlers(@Nonnull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
