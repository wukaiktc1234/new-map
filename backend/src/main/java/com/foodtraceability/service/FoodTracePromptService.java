package com.foodtraceability.service;

import com.foodtraceability.config.PromptAssemblyConfig.PromptBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FoodTracePromptService {

    public String buildSystemPrompt(String userRole, String language, boolean enableCache) {
        PromptBuilder builder = new PromptBuilder();

        builder.addStaticSection("identity", 
            "你是食品溯源系统的AI助手，专门帮助用户管理食品溯源、库存和供应链信息。")
            .addStaticSection("safety_rules",
            "安全规则：\n" +
            "1. 不要修改或删除用户未授权的数据\n" +
            "2. 所有数据库操作必须经过权限验证\n" +
            "3. 敏感信息必须加密存储\n" +
            "4. 操作日志必须完整记录")
            .addStaticSection("format_rules",
            "输出格式规范：\n" +
            "- 使用JSON格式返回结构化数据\n" +
            "- 日期格式：yyyy-MM-dd HH:mm:ss\n" +
            "- 金额格式：保留两位小数\n" +
            "- 状态字段：active/inactive/probation")
            .addStaticSection("behavior_policy",
            "行为准则：\n" +
            "1. 只执行用户明确要求的操作\n" +
            "2. 不要添加未请求的功能\n" +
            "3. 修改代码前必须先读取\n" +
            "4. 测试通过后才能报告成功");

        builder.addDynamicSection("user_context",
            String.format("当前用户角色：%s\n语言偏好：%s", userRole, language),
            Arrays.asList("user_role", "language"))
            .addDynamicSection("environment",
            "系统环境：\n" +
            "- 后端服务：http://localhost:8081/api\n" +
            "- 前端服务：http://localhost:3000\n" +
            "- 数据库：PostgreSQL 18\n" +
            "- 缓存：Redis",
            Arrays.asList("server_config"))
            .addDynamicSection("skill_discovery",
            "可用技能：\n" +
            "- 库存管理：查询、更新库存信息\n" +
            "- 溯源查询：追踪食品来源和流向\n" +
            "- 报表生成：生成库存、销售报表\n" +
            "- 预警通知：库存不足、过期提醒",
            Arrays.asList("enabled_skills"))
            .addDynamicSection("token_budget",
            String.format("Token预算：本次对话最多使用%d tokens", enableCache ? 4000 : 8000),
            Arrays.asList("cache_enabled"));

        return builder.build();
    }

    public Map<String, Object> getPromptSectionMap() {
        PromptBuilder builder = new PromptBuilder();
        
        builder.addStaticSection("identity", "身份定义")
            .addStaticSection("safety_rules", "安全规则")
            .addStaticSection("format_rules", "格式规范")
            .addStaticSection("behavior_policy", "行为准则")
            .addDynamicSection("user_context", "用户上下文", Arrays.asList("user_role", "language"))
            .addDynamicSection("environment", "环境信息", Arrays.asList("server_config"))
            .addDynamicSection("skill_discovery", "技能发现", Arrays.asList("enabled_skills"))
            .addDynamicSection("token_budget", "Token预算", Arrays.asList("cache_enabled"));

        return builder.getSectionMap();
    }

    public Map<String, Object> analyzeCacheImpact(String sectionId) {
        Map<String, Object> analysis = new HashMap<>();
        
        Map<String, List<String>> dependencyGraph = new HashMap<>();
        dependencyGraph.put("user_context", Arrays.asList("user_role", "language"));
        dependencyGraph.put("environment", Arrays.asList("server_config"));
        dependencyGraph.put("skill_discovery", Arrays.asList("enabled_skills"));
        dependencyGraph.put("token_budget", Arrays.asList("cache_enabled"));

        List<String> staticSections = Arrays.asList("identity", "safety_rules", "format_rules", "behavior_policy");
        
        if (staticSections.contains(sectionId)) {
            analysis.put("type", "STATIC");
            analysis.put("cacheable", true);
            analysis.put("impact", "修改此section会破坏整个缓存前缀");
        } else {
            analysis.put("type", "DYNAMIC");
            analysis.put("cacheable", false);
            analysis.put("dependencies", dependencyGraph.getOrDefault(sectionId, Collections.emptyList()));
            analysis.put("impact", "修改此section不会影响缓存前缀");
        }

        return analysis;
    }
}
