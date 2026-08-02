# 产品管理模块自动化测试指南

## 📋 测试覆盖范围

本测试套件包含 **5 大类、30+ 个测试用例**，全面覆盖产品管理模块的核心功能：

### 1️⃣ Mock 数据服务 - CRUD 操作 (15个测试)
- ✅ 分页查询（默认参数、关键词搜索、分类筛选、状态筛选）
- ✅ 空结果处理
- ✅ 分页逻辑验证
- ✅ 根据 ID 获取详情（成功/失败）
- ✅ 创建菜品（基本功能、自动编码、利润计算、列表位置）
- ✅ 更新菜品（成功更新、不存在ID、部分字段保持）
- ✅ 删除菜品（成功删除、不存在ID）
- ✅ 批量删除（正常删除、空数组）
- ✅ 状态更新（在售/停售/售罄）
- ✅ 辅助查询（listOnSale, listByCategory）

### 2️⃣ 导入导出功能 (8个测试)
- ✅ JSON 文件导入（有效数据、缺少必填字段、单条对象格式、数据验证）
- ✅ 无效 JSON 格式处理
- ✅ CSV 导出（Blob 对象、CSV 格式、完整字段、筛选条件）
- ✅ 模板下载（标准模板、示例数据）

### 3️⃣ 数据验证逻辑 (5个测试)
- ✅ 价格有效性验证（售价/成本 >= 0，售价 >= 成本）
- ✅ 利润率计算准确性
- ✅ 库存预警判断正确性
- ✅ 菜品编码格式统一性
- ✅ 时间戳 ISO 8601 格式

### 4️⃣ 工具函数 (3个测试)
- ✅ 分类选项返回值结构
- ✅ 属性类型检查
- ✅ 预定义分类完整性

### 5️⃣ 边界情况和异常处理 (4个测试)
- ✅ 大批量导入（100条）
- ✅ 特殊字符处理（引号、逗号、换行）
- ✅ 并发操作安全性
- ✅ 极端价格值处理

### 6️⃣ 性能基准测试 (2个测试)
- ✅ 列表查询响应时间 (< 500ms)
- ✅ 批量导入性能 (< 2000ms)

---

## 🚀 快速开始

### 前置条件

确保已安装以下依赖：

```bash
# 安装 Vitest 测试框架
npm install -D vitest @vue/test-utils jsdom
```

### 运行所有测试

```bash
# 运行产品管理模块的所有测试
npx vitest run src/api/product/__tests__/product.test.ts

# 或使用 npm script（如果已配置）
npm test:product
```

### 运行特定测试套件

```bash
# 只运行 CRUD 操作测试
npx vitest run src/api/product/__tests__/product.test.ts -t "CRUD"

# 只运行导入导出测试
npx vitest run src/api/product/__tests__/product.test.ts -t "导入导出"

# 只运行数据验证测试
npx vitest run src/api/product/__tests__/product.test.ts -t "数据验证"
```

### 监视模式（开发时使用）

```bash
# 文件变化时自动重新运行测试
npx vitest watch src/api/product/__tests__/product.test.ts
```

### 生成覆盖率报告

```bash
# 运行测试并生成覆盖率报告
npx vitest run --coverage src/api/product/__tests__/product.test.ts

# 报告输出在 coverage/ 目录
```

---

## 📊 预期输出示例

```
✓ src/api/product/__tests__/product.test.ts (30 tests) 1234ms

 ✓ Mock 数据服务 - CRUD 操作 > getList (5 tests) 234ms
   ✓ 应该返回分页数据，默认每页10条
   ✓ 应该支持关键词搜索
   ✓ 应该支持分类筛选
   ✓ 应该支持状态筛选
   ✓ 应该正确处理空结果
   ✓ 应该支持分页参数

 ✓ Mock 数据服务 - CRUD 操作 > getById (2 tests) 45ms
   ✓ 应该返回指定ID的菜品详情
   ✓ 当ID不存在时应该抛出错误

 ... (更多测试)

✓ 导入导出功能 > importFromJson (5 tests) 567ms
   ✓ 应该成功导入有效的JSON文件
   ✓ 应该拒绝缺少必填字段的记录
   ... (更多测试)

 Test Files  1 passed (1)
     Tests  30 passed (30)
  Duration  1.234s
```

---

## 🔧 配置选项（可选）

### 在 `vite.config.ts` 中添加配置

```typescript
import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: ['node_modules/', '__tests__/'],
    },
  },
})
```

### 在 `package.json` 中添加脚本

```json
{
  "scripts": {
    "test": "vitest",
    "test:ui": "vitest --ui",
    "test:coverage": "vitest run --coverage",
    "test:product": "vitest run src/api/product/__tests__/product.test.ts"
  }
}
```

---

## 📝 自定义测试用例

### 添加新的测试场景

在 `product.test.ts` 文件末尾添加：

```typescript
describe('你的自定义测试套件', () => {
  it('描述你的测试场景', async () => {
    // 准备测试数据
    const testData = { /* ... */ }

    // 执行操作
    const result = await someFunction(testData)

    // 断言结果
    expect(result).toBe(expectedValue)
  })
})
```

### 最佳实践

1. **命名清晰**：使用 `应该 + 动作 + 预期结果` 格式
2. **单一职责**：每个测试只验证一个行为
3. **独立性**：测试之间不共享状态（使用 beforeEach 重置）
4. **边界测试**：包含正常情况、边界值、异常情况
5. **可读性**：添加清晰的注释说明测试意图

---

## ⚠️ 常见问题排查

### 问题 1：找不到 `vitest` 命令

**解决方案**：
```bash
# 安装 vitest 到项目
npm install -D vitest

# 使用 npx 运行
npx vitest run ...
```

### 问题 2：测试超时

**解决方案**：
增加超时时间（默认 5000ms）：

```typescript
it('耗时较长的测试', async () => {
  // 设置 10 秒超时
}, 10000)
```

### 问题 3：Mock 数据被污染

**解决方案**：
确保每个测试前调用 `resetMockData()`：

```typescript
beforeEach(() => {
  resetMockData()
})
```

### 问题 4：ECharts 相关错误

**注意**：
当前测试文件不包含 ECharts 图表的 UI 测试。
图表测试需要 `@vue/test-utils` 和 DOM 环境，
建议单独创建组件测试文件。

---

## 📚 相关文档

- [Vitest 官方文档](https://vitest.dev/)
- [Vue Test Utils](https://test-utils.vuejs.org/)
- [Jest 匹配器 API](https://jestjs.io/docs/using-matchers)

---

## 🎯 下一步建议

1. **集成到 CI/CD**：在 GitHub Actions / GitLab CI 中添加测试步骤
2. **代码覆盖率目标**：达到 80%+ 的分支覆盖率
3. **E2E 测试**：使用 Playwright/Cypress 进行端到端测试
4. **快照测试**：对导出的 CSV 文件进行快照对比
5. **性能回归检测**：定期运行性能基准测试并记录结果

---

**最后更新**: 2025-05-11
**维护者**: AI Assistant
**版本**: 1.0.0
