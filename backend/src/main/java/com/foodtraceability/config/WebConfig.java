package com.foodtraceability.config;

import com.foodtraceability.interceptor.RateLimitInterceptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Web配置类，处理静态资源和跨域请求
 * 
 * @author demo
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 配置静态资源处理
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 配置静态资源映射，只处理/static/**路径
        // 确保不拦截API路径（/api/**）
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true);
    }

    /**
     * 配置路径匹配策略，确保控制器路径优先于静态资源
     */
    @Override
    @SuppressWarnings("deprecation")
    public void configurePathMatch(@NonNull org.springframework.web.servlet.config.annotation.PathMatchConfigurer configurer) {
        // 使用路径前缀匹配，确保控制器路径优先
        configurer.setUseTrailingSlashMatch(true);
    }

    /**
     * 配置视图控制器，确保API路径不被当作静态资源
     */
    @Override
    public void addViewControllers(@NonNull org.springframework.web.servlet.config.annotation.ViewControllerRegistry registry) {
        // 不添加任何视图控制器，让所有API路径由控制器处理
    }

    /**
     * 配置HTTP消息转换器，确保响应使用UTF-8编码
     */
    @Override
    @SuppressWarnings("nullness")
    public void configureMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 关键：禁用对未知字段的反序列化失败校验。
        // Spring Boot application.yml 中的 spring.jackson.deserialization.fail_on_unknown_properties=false
        // 仅对 Spring Boot 自动配置的 ObjectMapper 生效；本类手动 new ObjectMapper() 会覆盖默认配置，
        // 必须显式 disable(FAIL_ON_UNKNOWN_PROPERTIES) 才能容忍请求体中的多余字段（如前端透传的 productName）。
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(java.time.LocalDateTime.class, 
            new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(java.time.LocalDateTime.class, 
            new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        jsonConverter.setObjectMapper(objectMapper);
        jsonConverter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_PLAIN,
            new MediaType("application", "json", StandardCharsets.UTF_8),
            new MediaType("text", "json", StandardCharsets.UTF_8)
        ));
        jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
        
        converters.add(0, jsonConverter);
        
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        converters.add(1, stringConverter);
    }

    /**
     * 配置Kaptcha验证码生成器
     */
    /**
     * 配置API请求频率限制拦截器
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 注意：由于context-path设置为/api，服务器看到的路径不包含/api前缀
        // 添加API请求频率限制拦截器
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/v1/hardware/device/**")
                .excludePathPatterns("/v1/auth/**")
                .excludePathPatterns("/auth/**")
                .excludePathPatterns("/public/**")
                .excludePathPatterns("/onboarding/invitation/**")
                .excludePathPatterns("/actuator/**")
                .excludePathPatterns("/swagger-ui.html")
                .excludePathPatterns("/v2/api-docs")
                // 审批流程查询（模式切换时并行加载多个业务类型，避免误触限流）
                .excludePathPatterns("/v1/approval/**");
    }
    
    @Bean
    public Producer captchaProducer() {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", "no");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.textproducer.char.space", "5");
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        properties.setProperty("kaptcha.image.width", "120");
        properties.setProperty("kaptcha.image.height", "40");
        properties.setProperty("kaptcha.textproducer.font.size", "30");
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }
}
