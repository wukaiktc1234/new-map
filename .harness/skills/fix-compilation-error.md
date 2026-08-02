# Skill: 修复后端编译错误

> 触发条件：`mvn compile` 失败，出现 Java 编译错误

## 标准排查路径（按优先级）

### Step 1: 快速定位错误类型
```
运行: mvn compile 2>&1 | grep "error:" | head -20
分类错误:
  - [符号找不到] → 缺少 import 或 Lombok 残留
  - [类型不匹配] → 类型转换或泛型问题
  - [非法转义] → 正则表达式中的反斜杠
  - [重复类/接口] → 同文件多 public 类型
  - [包不存在] → 依赖缺失或 artifactId 被禁用
```

### Step 2: Lombok 残留检查（本项目特有）
```
本项目已 Delombok，以下残留会导致编译失败:
1. import lombok.*; → 删除
2. @Data, @Slf4j, @Builder 等注解 → 已展开为显式代码
3. @lombok.Generated 注解 → 必须删除（9848处已清理）
4. pom.xml 中 lombok_DISABLED → 确认保持禁用状态
```

### Step 3: 常见修复模式
| 错误模式 | 修复方式 |
|---------|---------|
| `cannot find symbol` | 添加正确的 import 或使用全限定名 |
| `method does not override` | 检查父类方法签名是否匹配 |
| `incompatible types` | 显式类型转换 (Long.parseLong, Integer.valueOf) |
| `illegal escape character` | 正则中的 \ 需要写为 \\ |
| `package X does not exist` | 检查 pom.xml 依赖是否存在 |

### Step 4: 批量修复策略
```
单文件错误 (<5个): 直接 SearchReplace 修复
同类错误 (>10个): 使用 java-expert subagent 批量处理
跨模块错误: 先修根因（如公共类），再重编译验证
```

### Step 5: 验证
```bash
mvn compile -q && echo "BUILD SUCCESS"
# 确认 0 errors, 0 warnings(可选)
```

## 本项目已知陷阱
- AnomalyAccessLogFilter.java 曾因缺少 copyBodyToResponse() 导致所有API返回空body
- DatabaseInitConfig.java 的 ROLE_CODE='Z' 应为 'admin'
- ConfigManagement.vue 的 `<div="table-toolbar">` 缺少 class 关键字

## 完成标志
- [ ] mvn compile 输出 BUILD SUCCESS
- [ ] 错误数 = 0
- [ ] 更新 PROJECT_PROGRESS.md 版本历史
