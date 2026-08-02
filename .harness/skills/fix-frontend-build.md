# Skill: 修复前端编译错误

> 触发条件：`npm run build` 或 Vite 编译失败

## 标准排查路径

### Step 1: 定位错误类型
```
npm run build 2>&1 | Select-String "error|Error"

常见错误分类:
├── Vue模板语法错误
│   ├── 缺少闭合标签
│   ├── 属性语法错误: <div="xxx"> 应为 <div class="xxx">
│   ├── v-if/v-for 用法错误
│   └── 模板中使用了未定义的变量/方法
│
├── TypeScript 类型错误
│   ├── any 类型禁止使用
│   ├── import 路径错误
│   ├── 接口属性不匹配
│   └── 未使用的变量/导入
│
├── CSS/SCSS 错误
│   ├── 语法错误
│   └── 文件超过600行限制
│
└── 引用错误
    ├── 组件文件不存在
    └── 路径大小写不一致 (Linux敏感)
```

### Step 2: Vue模板常见问题
```vue
<!-- ❌ 错误 -->
<div="container">
<img :src="url">

<!-- ✅ 正确 -->
<div class="container">
<img :src="url" alt="" />
```

**本项目已知模板BUG:**
- ConfigManagement.vue 第41行: `<div="table-toolbar">` → 缺少 `class`
- Security.vue: Lock 导入重复 (line 183+186)

### Step 3: TypeScript 严格模式
```
项目规则: 禁止 any 类型

替代方案:
any       → unknown 或具体类型
as any    → 类型断言 (尽量避免)
let x: any → let x: SpecificType
```

### Step 4: 路由引用检查
```
router/index.ts 中的 component 路径必须与实际文件一致:

正确示例:
component: () => import("@/views/SystemSettings/PermissionCenter/UserManagement.vue")

常见错误:
- 文件在 components/ 子目录但路径未包含
- 文件名大小写不匹配 (UserRoles.vue ≠ userroles.vue)
- 文件根本不存在 (404)
```

### Step 5: 文件长度检查
```
项目限制:
- Vue组件: ≤1000行
- TypeScript: ≤800行  
- SCSS/CSS: ≤600行

超限处理: 拆分为子组件或提取 composables
```

## 快速修复命令
```bash
# 查看具体错误位置
cd frontend && npm run build 2>&1 | Select-String "error" | Select-Object -First 10

# TypeScript 检查
cd frontend && npx vue-tsc --noEmit 2>&1 | Select-Object -First 20
```

## 完成标志
- [ ] npm run build 输出 SUCCESS (0 errors)
- [ ] 无 any 类型警告
- [ ] 所有组件文件 < 1000行
- [ ] 浏览器热更新正常
