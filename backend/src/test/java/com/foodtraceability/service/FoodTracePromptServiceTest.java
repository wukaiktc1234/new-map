package com.foodtraceability.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FoodTracePromptServiceTest {

    @Autowired
    private FoodTracePromptService promptService;

    @Test
    public void testBuildSystemPrompt() {
        String prompt = promptService.buildSystemPrompt("管理员", "zh-CN", true);
        
        assertNotNull(prompt);
        assertTrue(prompt.contains("食品溯源系统"));
        assertTrue(prompt.contains("管理员"));
        assertTrue(prompt.contains("zh-CN"));
        assertTrue(prompt.contains("<DYNAMIC_BOUNDARY/>"));
        
        System.out.println("=== 生成的系统提示词 ===");
        System.out.println(prompt);
    }

    @Test
    public void testGetPromptSectionMap() {
        Map<String, Object> sectionMap = promptService.getPromptSectionMap();
        
        assertNotNull(sectionMap);
        assertTrue(sectionMap.containsKey("identity"));
        assertTrue(sectionMap.containsKey("safety_rules"));
        assertTrue(sectionMap.containsKey("user_context"));
        
        System.out.println("\n=== 提示词Section映射 ===");
        sectionMap.forEach((id, info) -> {
            System.out.println(id + ": " + info);
        });
    }

    @Test
    public void testAnalyzeCacheImpact() {
        Map<String, Object> staticAnalysis = promptService.analyzeCacheImpact("identity");
        assertEquals("STATIC", staticAnalysis.get("type"));
        assertEquals(true, staticAnalysis.get("cacheable"));
        
        Map<String, Object> dynamicAnalysis = promptService.analyzeCacheImpact("user_context");
        assertEquals("DYNAMIC", dynamicAnalysis.get("type"));
        assertEquals(false, dynamicAnalysis.get("cacheable"));
        
        System.out.println("\n=== 缓存影响分析 ===");
        System.out.println("Static Section (identity): " + staticAnalysis);
        System.out.println("Dynamic Section (user_context): " + dynamicAnalysis);
    }

    @Test
    public void testPromptCachingStrategy() {
        String cachedPrompt = promptService.buildSystemPrompt("管理员", "zh-CN", true);
        String uncachedPrompt = promptService.buildSystemPrompt("管理员", "zh-CN", false);
        
        int boundaryIndex = cachedPrompt.indexOf("<DYNAMIC_BOUNDARY/>");
        String staticPart = cachedPrompt.substring(0, boundaryIndex);
        
        assertTrue(cachedPrompt.contains("4000 tokens"));
        assertTrue(uncachedPrompt.contains("8000 tokens"));
        
        System.out.println("\n=== 缓存策略对比 ===");
        System.out.println("启用缓存时的Token预算: 4000");
        System.out.println("未启用缓存时的Token预算: 8000");
        System.out.println("静态部分长度: " + staticPart.length() + " 字符");
    }
}
