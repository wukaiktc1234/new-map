package com.foodtraceability.config;

import com.foodtraceability.filter.XssFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 过滤器配置类
 * 用于注册自定义过滤器
 */
@Configuration
public class FilterConfig {

    /**
     * 注册XSS过滤器
     */
    @Bean
    public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(1); // 优先级，数字越小优先级越高
        return registration;
    }
}
