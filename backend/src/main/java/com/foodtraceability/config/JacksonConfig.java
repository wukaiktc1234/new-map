package com.foodtraceability.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jackson 全局配置
 *
 * 序列化（输出）：统一使用 yyyy-MM-dd HH:mm:ss 格式，便于前端展示
 * 反序列化（输入）：兼容以下两种格式
 *   - yyyy-MM-dd HH:mm:ss（空格分隔，与序列化格式一致）
 *   - yyyy-MM-dd'T'HH:mm:ss（ISO 8601，前端 el-date-picker/value-format 转换后的格式）
 */
@Configuration
public class JacksonConfig {

    /** 序列化/输出格式（前端展示友好） */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter serializerFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

        // 序列化：统一输出 yyyy-MM-dd HH:mm:ss
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(serializerFormatter));

        // 反序列化：兼容 yyyy-MM-dd HH:mm:ss 和 ISO 8601 两种格式
        javaTimeModule.addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());

        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Long 类型序列化为字符串，避免前端精度丢失
        objectMapper.registerModule(new com.fasterxml.jackson.databind.module.SimpleModule()
            .addSerializer(Long.class, new ToStringSerializer(Long.class))
            .addSerializer(Long.TYPE, new ToStringSerializer(Long.TYPE)));

        return objectMapper;
    }

    /**
     * 灵活的 LocalDateTime 反序列化器
     * 支持 yyyy-MM-dd HH:mm:ss 和 yyyy-MM-dd'T'HH:mm:ss 两种格式
     */
    private static class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

        private static final long serialVersionUID = 1L;

        private static final DateTimeFormatter FORMATTER_SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        FlexibleLocalDateTimeDeserializer() {
            super(LocalDateTime.class);
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.getCurrentToken() == JsonToken.VALUE_NULL) {
                return null;
            }
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }
            // 尝试空格分隔格式
            try {
                return LocalDateTime.parse(value, FORMATTER_SPACE);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种格式
            }
            // 尝试 ISO 8601 格式（带 'T' 分隔符）
            try {
                return LocalDateTime.parse(value, FORMATTER_ISO);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种格式
            }
            // 兜底：尝试 JDK 默认 ISO 解析（支持带毫秒/纳秒的 ISO 8601）
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException e) {
                throw ctxt.weirdStringException(value, LocalDateTime.class,
                    "无法解析为 LocalDateTime，支持的格式: yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd'T'HH:mm:ss");
            }
        }
    }
}
