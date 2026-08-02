package com.foodtraceability.config;

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

    private final ClientTypeInterceptor clientTypeInterceptor;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(@Nonnull InterceptorRegistry registry) {
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
