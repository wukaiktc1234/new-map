# SKILL 使用指南：Prompt Assembly Architecture

## 一、SKILL 概述

**SKILL 名称**: prompt-assembly-architecture  
**适用场景**: 设计、审查或重构提示词组装系统  
**核心价值**: 将提示词构建视为流水线，而非临时字符串拼接

## 二、核心概念

### 1. 静态区（Static Zone）
- **特点**: 跨会话稳定，可缓存
- **内容**: 身份定义、安全规则、格式规范、行为准则
- **优势**: 减少Token消耗，提高响应速度

### 2. 动态区（Dynamic Zone）
- **特点**: 每次请求可能变化，不可缓存
- **内容**: 用户上下文、环境信息、技能发现、Token预算
- **优势**: 灵活适应不同场景

### 3. 边界标记（Boundary Marker）
```
<DYNAMIC_BOUNDARY/>
```
- 明确区分静态和动态部分
- 便于缓存层、调试工具和API层统一处理

## 三、实际应用示例

### 示例1：构建食品溯源系统提示词

```java
FoodTracePromptService promptService = new FoodTracePromptService();

// 构建系统提示词
String systemPrompt = promptService.buildSystemPrompt(
    "管理员",    // 用户角色
    "zh-CN",     // 语言偏好
    true         // 启用缓存
);

System.out.println(systemPrompt);
```

**输出结果**:
```
你是食品溯源系统的AI助手，专门帮助用户管理食品溯源、库存和供应链信息。

安全规则：
1. 不要修改或删除用户未授权的数据
2. 所有数据库操作必须经过权限验证
3. 敏感信息必须加密存储
4. 操作日志必须完整记录

输出格式规范：
- 使用JSON格式返回结构化数据
- 日期格式：yyyy-MM-dd HH:mm:ss
- 金额格式：保留两位小数
- 状态字段：active/inactive/probation

行为准则：
1. 只执行用户明确要求的操作
2. 不要添加未请求的功能
3. 修改代码前必须先读取
4. 测试通过后才能报告成功

<DYNAMIC_BOUNDARY/>

当前用户角色：管理员
语言偏好：zh-CN

系统环境：
- 后端服务：http://localhost:8081/api
- 前端服务：http://localhost:3000
- 数据库：PostgreSQL 18
- 缓存：Redis

可用技能：
- 库存管理：查询、更新库存信息
- 溯源查询：追踪食品来源和流向
- 报表生成：生成库存、销售报表
- 预警通知：库存不足、过期提醒

Token预算：本次对话最多使用4000 tokens
```

### 示例2：分析缓存影响

```java
// 分析静态Section的缓存影响
Map<String, Object> staticAnalysis = promptService.analyzeCacheImpact("identity");
// 输出: {type=STATIC, cacheable=true, impact=修改此section会破坏整个缓存前缀}

// 分析动态Section的缓存影响
Map<String, Object> dynamicAnalysis = promptService.analyzeCacheImpact("user_context");
// 输出: {type=DYNAMIC, cacheable=false, dependencies=[user_role, language], impact=修改此section不会影响缓存前缀}
```

## 四、设计规则应用

### ✅ 正确做法

1. **保持静态前缀尽可能长且稳定**
```java
builder.addStaticSection("identity", "...")
       .addStaticSection("safety_rules", "...")
       .addStaticSection("format_rules", "...")
       .addStaticSection("behavior_policy", "...");
```

2. **分离身份定义、行为策略和工具指导**
```java
// 身份定义
addStaticSection("identity", "你是食品溯源系统的AI助手...")

// 行为策略
addStaticSection("behavior_policy", "只执行用户明确要求的操作...")

// 工具指导
addDynamicSection("skill_discovery", "可用技能：...", dependencies)
```

3. **将运行时变量视为一等风险源**
```java
addDynamicSection("user_context", 
    String.format("当前用户角色：%s\n语言偏好：%s", userRole, language),
    Arrays.asList("user_role", "language")  // 明确依赖关系
)
```

### ❌ 错误做法

1. **在构建静态前缀时读取运行时状态**
```java
// ❌ 错误：在静态区读取用户信息
addStaticSection("identity", "你是" + getCurrentUser() + "的助手...");
```

2. **先生成一个巨大的字符串再尝试分割**
```java
// ❌ 错误：后期分割
String fullPrompt = buildEverything();
String[] parts = fullPrompt.split("BOUNDARY");
```

3. **让功能开关在静态前缀中繁殖**
```java
// ❌ 错误：在静态区使用条件判断
if (featureEnabled("NEW_FEATURE")) {
    addStaticSection("new_feature", "...");
}
```

## 五、性能优化建议

### 1. 缓存策略

| 场景 | Token预算 | 缓存策略 |
|------|----------|---------|
| 启用缓存 | 4000 tokens | 静态部分可复用 |
| 未启用缓存 | 8000 tokens | 每次重新计算 |

### 2. Section依赖管理

```java
// 建立依赖图
Map<String, List<String>> dependencyGraph = new HashMap<>();
dependencyGraph.put("user_context", Arrays.asList("user_role", "language"));
dependencyGraph.put("environment", Arrays.asList("server_config"));
dependencyGraph.put("skill_discovery", Arrays.asList("enabled_skills"));
dependencyGraph.put("token_budget", Arrays.asList("cache_enabled"));
```

### 3. 缓存失效追踪

```java
// 记录每个动态Section的依赖输入
public Map<String, Object> analyzeCacheImpact(String sectionId) {
    // 分析修改某个Section对缓存的影响
    // 返回：类型、是否可缓存、依赖项、影响范围
}
```

## 六、测试验证

运行测试用例验证实现：

```bash
mvn test -Dtest=FoodTracePromptServiceTest
```

测试覆盖：
- ✅ 系统提示词生成
- ✅ Section映射获取
- ✅ 缓存影响分析
- ✅ 缓存策略对比

## 七、最佳实践总结

1. **明确边界**: 使用 `<DYNAMIC_BOUNDARY/>` 标记静态和动态区
2. **稳定优先**: 保持静态前缀尽可能长且不变
3. **依赖追踪**: 记录每个动态Section的依赖关系
4. **缓存意识**: 将缓存行为作为显式设计关注点
5. **测试驱动**: 编写测试验证提示词组装逻辑

## 八、相关 SKILL

- **prompt-cache-economics**: 提示词缓存经济学
- **behavior-institutionalization**: 行为制度化
- **context-hygiene-system**: 上下文卫生系统

## 九、参考资源

- Claude Code 源码: `src/constants/prompts.ts`
- 系统提示词边界: `SYSTEM_PROMPT_DYNAMIC_BOUNDARY`
- 会话指导: `getSessionSpecificGuidanceSection()`
