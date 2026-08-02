package com.foodtraceability.aspect;

import com.foodtraceability.annotation.Desensitize;
import com.foodtraceability.annotation.DesensitizeType;
import com.foodtraceability.utils.DesensitizeUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 数据脱敏切面
 * 用于自动处理带有@Desensitize注解的字段
 */
@Aspect
@Component
public class DesensitizeAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(DesensitizeAspect.class);
    
    /**
     * 定义切点，拦截所有返回Result类型的方法
     */
    @Pointcut("execution(* com.foodtraceability.controller..*.*(..))")
    public void desensitizePointcut() {
    }
    
    /**
     * 环绕通知，实现数据脱敏逻辑
     */
    @Around("desensitizePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 执行原始方法
        Object result = joinPoint.proceed();
        
        // 如果返回结果为null，直接返回
        if (result == null) {
            return result;
        }
        
        // 对返回结果进行脱敏处理
        desensitizeObject(result);
        
        return result;
    }
    
    /**
     * 对对象进行脱敏处理
     * @param obj 需要脱敏的对象
     */
    private void desensitizeObject(Object obj) {
        if (obj == null) {
            return;
        }
        
        // 如果是集合类型，递归处理每个元素
        if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            for (Object item : collection) {
                desensitizeObject(item);
            }
            return;
        }
        
        // 如果是Map类型，递归处理每个值
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Object value : map.values()) {
                desensitizeObject(value);
            }
            return;
        }
        
        // 如果是数组类型，递归处理每个元素
        if (obj.getClass().isArray()) {
            // 跳过基本类型数组（如 byte[]）
            if (obj.getClass().getComponentType().isPrimitive()) {
                return;
            }
            Object[] array = (Object[]) obj;
            for (Object item : array) {
                desensitizeObject(item);
            }
            return;
        }
        
        // 处理普通对象
        Class<?> clazz = obj.getClass();
        
        // 获取所有字段（包括父类字段）
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        
        // 遍历所有字段，对带有@Desensitize注解的字段进行脱敏处理
        for (Field field : fields) {
            Desensitize desensitize = field.getAnnotation(Desensitize.class);
            if (desensitize != null) {
                try {
                    // 设置字段可访问
                    field.setAccessible(true);
                    
                    // 获取字段值
                    Object fieldValue = field.get(obj);
                    if (fieldValue instanceof String) {
                        String value = (String) fieldValue;
                        String desensitizedValue;
                        
                        // 根据脱敏类型进行处理
                        if (desensitize.value() == DesensitizeType.DEFAULT) {
                            // 默认脱敏，根据字段名自动判断类型
                            String fieldName = field.getName();
                            if (fieldName.contains("phone") || fieldName.contains("mobile")) {
                                desensitizedValue = DesensitizeUtils.desensitizePhone(value);
                            } else if (fieldName.contains("email")) {
                                desensitizedValue = DesensitizeUtils.desensitizeEmail(value);
                            } else if (fieldName.contains("idCard") || fieldName.contains("idNumber")) {
                                desensitizedValue = DesensitizeUtils.desensitizeIdCard(value);
                            } else if (fieldName.contains("name") || fieldName.contains("username")) {
                                desensitizedValue = DesensitizeUtils.desensitizeName(value);
                            } else if (fieldName.contains("address")) {
                                desensitizedValue = DesensitizeUtils.desensitizeAddress(value);
                            } else if (fieldName.contains("bankCard") || fieldName.contains("cardNumber")) {
                                desensitizedValue = DesensitizeUtils.desensitizeBankCard(value);
                            } else {
                                // 自定义脱敏
                                desensitizedValue = DesensitizeUtils.desensitizeCustom(value, desensitize.start(), desensitize.end(), desensitize.replacement());
                            }
                        } else {
                            // 指定脱敏类型
                            desensitizedValue = DesensitizeUtils.desensitizeByType(value, desensitize.value());
                        }
                        
                        // 设置脱敏后的值
                        field.set(obj, desensitizedValue);
                    }
                } catch (IllegalAccessException e) {
                    logger.error("数据脱敏失败: {}", e.getMessage());
                }
            }
        }
    }
}