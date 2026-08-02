# 多会话任务分派方案

> 创建时间：2026-04-05
> 适用版本：v0.5.0+
> 状态：🟢 已激活（3个会话窗口并行中）

---

## 一、项目生态全景

```
p:\my-new-project\
│
├── backend/              Spring Boot 3.2.0 后端API（所有终端共用）
├── frontend/             Vue.js 3.3 管理后台前端（当前主力）
├── frontend-pos/         收银端前端（已有基础框架）
├── frontend-kitchen/     后厨端前端（骨架阶段）
├── miniprogram/          微信小程序（骨架阶段）
├── ocr-service/          OCR发票识别服务(Python/FastAPI)
├── PostgreSQL/18/        共享数据库实例
├── (硬件设备)            打印机/小票机/扫码枪/厨房显示屏（规划中）
│
└── .harness/             协调基础设施
    ├── PROTOCOL.md       Agent工作协议
    ├── MULTI_SESSION.md  多会话协调协议
    └── SESSION_ASSIGNMENT.md ← 本文档
```

**架构本质**: 星型架构 — 一个共享后端 + N个前端触角

---

## 二、三会话分工总览

```
                        ┌─────────────────────────────────┐
                        │     共享资源层（只读/协调）        │
                        │  PROJECT_PROGRESS.md            │
                        │  .harness/{lock,inbox,skills}   │
                        │  backend/src (API契约定义)       │
                        └──────┬──────────┬───────────────┘
                               │          │
                    ┌──────────▼──┐  ┌────▼──────────┐
                    │   会话B     │  │    会话C       │
                    │  前端矩阵   │  │  后端+服务     │
                    │             │  │               │
                    │ • 管理后台   │  │ • Spring Boot  │
                    │ • 收银端     │  │ • OCR Service  │
                    │ • 后厨端     │  │ • DB Migration │
                    │ • 小程序     │  │ • 硬件驱动层   │
                    └─────────────┘  └────────────────┘
                               │
                    ┌──────────▼──┐
                    │   会话A     │
                    │  主控协调员  │
                    │             │
                    │ • 冒烟测试   │
                    │ • 集成验证   │
                    │ • 进度同步   │
                    │ • 冲突仲裁   │
                    └─────────────┘
```

---

## 三、各会话详细职责

### 会话A — 主控协调员（Master Coordinator）

| 属性 | 值 |
|------|-----|
| **定位** | 总调度、跨模块审查、冲突仲裁 |
| **文件所有权(写)** | `PROJECT_PROGRESS.md`、`.harness/` 目录全部 |
| **文件访问权(读)** | 全项目只读 |
| **禁止触碰** | 不直接修改业务代码 |

#### 核心任务清单

| 阶段 | 任务 | 验收标准 |
|------|------|---------|
| Phase 1 | 全局冒烟测试 — 启动后端+前端，逐一验证11个MVP页面 | 每页截图或状态记录 |
| Phase 1 | 财务模块可访问性确认 — 登录后检查菜单+页面加载 | 记录可访问/不可访问页面列表 |
| Phase 1 | 跨会话进度同步 — 每里程碑更新 PROJECT_PROGRESS.md | 版本号递增 |
| Phase 1 | 代码审查 — 审查B/C会话提交的改动 | 编译通过 + 符合规范 |
| Phase 2 | 全链路模拟测试 — 下单→后厨接单→打印小票→追溯查询 | 流程跑通记录 |
| Phase 2 | 硬件对接规划 — 梳理打印机/扫码枪/厨房屏接口协议 | 输出硬件对接方案文档 |
| Phase 3 | 端到端验收 — 全模块回归测试 | 测试报告 |
| Phase 3 | 生产部署准备 — 环境检查清单 | 部署检查表 |

#### 启动指令（复制给AI）

```
你是主控会话。读取以下文件了解项目全貌：
1. PROJECT_PROGRESS.md — 项目进度和待办事项
2. .harness/PROTOCOL.md — Agent工作协议
3. .harness/MULTI_SESSION.md — 多会话协调规则
4. .harness/SESSION_ASSIGNMENT.md — 本分派方案

你的职责：
(1) 启动后端(mvn spring-boot:run)和前端(npm run dev)，验证服务正常
(2) 逐一验证系统设置下11个MVP页面能否打开不报错
(3) 确认登录后财务模块菜单可见且页面可访问
(4) 跟踪并汇总其他会话的工作进度，更新PROJECT_PROGRESS.md
(5) 使用 .harness/skills/post-work-checklist.md 作为工作结束检查标准

协调工具：
- 会话锁: .harness/lock/session-lock.json
- 消息箱: .harness/inbox/
- 进度共享: PROJECT_PROGRESS.md
```

---

### 会话B — 前端矩阵工程师（Frontend Matrix Engineer）

| 属性 | 值 |
|------|-----|
| **定位** | 所有前端子系统的开发与完善 |
| **文件所有权(写)** | `frontend/`、`frontend-pos/`、`frontend-kitchen/`、`miniprogram/` |
| **禁止触碰** | `backend/` 目录、`pom.xml`、`.harness/`（除inbox发消息外）、`PROJECT_PROGRESS.md` |

#### 管理的4个前端子系统

| 子系统 | 路径 | 技术栈 | 当前状态 | 近期优先级 |
|--------|------|--------|---------|-----------|
| **管理后台** | `frontend/` | Vue 3 + Element Plus + TS | 🔴 重点攻坚期 | P0 最高 |
| **收银端** | `frontend-pos/` | Vue 3 + TS | 🟡 有基础框架 | P1 第二 |
| **后厨端** | `frontend-kitchen/` | Vue 3 | 🟢 骨架阶段 | P2 第三 |
| **微信小程序** | `miniprogram/` | 原生小程序 | 🟢 骨架阶段 | P2 第三 |

#### 任务清单（按阶段排序）

**Phase 1: 管理后台完善（当前最高优）**

| # | 任务 | 涉及文件 | 验收标准 |
|---|------|---------|---------|
| B-01 | 用户管理页面功能补全（表格+搜索+CRUD弹窗） | `views/SystemSettings/PermissionCenter/UserManagement.vue` | 列表展示/新增/编辑/删除/搜索均可用 |
| B-02 | 角色管理页面功能补全 | `views/SystemSettings/PermissionCenter/RoleManagement.vue` | 同上 |
| B-03 | 权限管理页面功能补全（树形结构+勾选） | `views/SystemSettings/PermissionCenter/PermissionManagement.vue` | 权限树展示+角色关联 |
| B-04 | 角色权限关联页面完善 | `views/SystemSettings/PermissionCenter/components/RolePermissions.vue` | 双栏选择交互 |
| B-05 | 用户角色关联页面完善 | `views/SystemSettings/PermissionCenter/components/UserRoles.vue` | 用户-角色多选绑定 |
| B-06 | 个人资料页面内容填充 | `views/SystemSettings/UserCenter/Profile.vue` | 表单可编辑+保存 |
| B-07 | 账号安全页面内容填充 | `views/SystemSettings/UserCenter/Security.vue` | 密码修改+登录日志 |
| B-08 | 偏好设置页面内容填充 | `views/SystemSettings/UserCenter/Preferences.vue` | 主题/语言等设置项 |
| B-09 | 组织架构页面完善 | `views/SystemSettings/OrganizationSettings.vue` | 组织树展示 |
| B-10 | 财务模块前端验证与修复 | `views/Finance/` 全部 | 页面可访问+数据正确展示 |

**Phase 2: 收银端开发**

| # | 任务 | 说明 |
|---|------|------|
| B-11 | 商品展示与分类浏览 | 商品列表/分类筛选/搜索 |
| B-12 | 购物车与下单结算 | 加购/数量调整/优惠计算/支付 |
| B-13 | 小票打印UI | 本地打印预览+打印触发 |
| B-14 | 会员管理与积分 | 会员识别/积分显示/充值 |
| B-15 | 交班与日结 | 交接班流程/日报表 |

**Phase 3: 后厨端 + 小程序**

| # | 任务 | 说明 |
|---|------|------|
| B-16 | 后厨订单看板 | 实时订单列表/菜品状态切换 |
| B-17 | 超时提醒与催菜 | 倒计时/超时高亮/催菜通知 |
| B-18 | 小程序扫码溯源 | 扫码→溯源信息展示 |
| B-19 | 小程序会员点餐 | 浏览/下单/支付 |

#### 启动指令（复制给AI）

```
你是前端矩阵会话。负责4个前端子系统的开发。

启动步骤：
1. 读取 PROJECT_PROGRESS.md 了解全局进度
2. 读取 .harness/PROTOCOL.md 了解工作协议
3. 读取 .harness/SESSION_ASSIGNMENT.md 了解你的任务清单
4. 加载相关 Skill: .harness/skills/fix-frontend-build.md

工作范围（仅限以下目录）：
- frontend/        → 管理后台（当前重点）
- frontend-pos/    → 收银端
- frontend-kitchen/ → 后厨端
- miniprogram/     → 微信小程序

绝对禁止修改：
- backend/         → 由会话C负责
- PROJECT_PROGRESS.md → 由会话A负责
- .harness/        → 仅可通过 inbox 发消息

当前最高优先级（Phase 1）：
完善 frontend/ 管理后台的系统设置MVP页面（11页）和财务模块。

必须遵循的前端规范：
- 使用 src/api/request.ts 导出的 request 实例发送请求
- 禁止直接使用 axios 或 fetch
- 禁止使用 any 类型
- Vue组件不超过1000行，TS文件不超过800行
- 响应数据直接使用（不访问.data属性）
- CSS变量统一样式，组件样式使用scoped

每完成一个子系统运行 npm run build 验证编译。
完成后通过 .harness/inbox/ 向其他会话发送完成通知。
```

---

### 会话C — 后端与服务工程师（Backend & Services Engineer）

| 属性 | 值 |
|------|-----|
| **定位** | Spring Boot API + OCR服务 + 数据库 + 硬件驱动层 |
| **文件所有权(写)** | `backend/`、`ocr-service/`、`config/`、SQL脚本、`pom.xml` |
| **禁止触碰** | `frontend/`、`frontend-pos/`、`frontend-kitchen/`、`miniprogram/`、`.harness/`（除inbox外）、`PROJECT_PROGRESS.md` |

#### 管理的后端与服务子系统

| 子系统 | 路径 | 技术栈 | 当前状态 | 近期优先级 |
|--------|------|--------|---------|-----------|
| **Spring Boot API** | `backend/` | Spring Boot 3.2 + MyBatis Plus + PostgreSQL | 🔴 核心服务运行中 | P0 最高 |
| **OCR识别服务** | `ocr-service/` | Python FastAPI + PaddleOCR | 🟡 可独立运行 | P1 第二 |
| **PostgreSQL** | `PostgreSQL/18/` | PostgreSQL 18 | 🟢 已安装可用 | P1 配套 |
| **硬件驱动层** | 待创建 | Java/串口/网络协议 | 🔵 规划中 | P2 远期 |

#### 任务清单（按阶段排序）

**Phase 1: API完善与基础设施（当前最高优）**

| # | 任务 | 涉及文件 | 验收标准 |
|---|------|---------|---------|
| C-01 | 用户管理CRUD API完整性验证与修复 | UserController/Service/Mapper | 分页列表/创建/更新/删除均可调用 |
| C-02 | 角色管理CRUD API完整性验证与修复 | RoleController/Service/Mapper | 同上 |
| C-03 | 权限管理API完整性验证与修复 | PermissionController/Service/Mapper | 权限树获取/分配可用 |
| C-04 | 用户-角色关联API验证 | UserRole相关接口 | 绑定/解绑正常 |
| C-05 | 角色-权限关联API验证 | RolePermission相关接口 | 权限分配正常 |
| C-06 | 确认所有Controller返回统一Result格式 | 各Controller | 无直接返回实体的情况 |
| C-07 | PostgreSQL正式迁移脚本编写 | 新建 `backend/src/main/resources/db/migration/` 或SQL文件 | Flyway V1脚本含所有表 |
| C-08 | 生产环境application-prod.yml配置 | `config/application-prod.yml` 或 `backend/src/main/resources/` | 含真实DB/Redis/JWT配置 |
| C-09 | JWT密钥改为环境变量注入 | SecurityConfig + yml | 禁止硬编码密钥 |
| C-10 | Redis真实连接配置 | CacheServiceImpl + yml | 从ConcurrentHashMap降级切回Redis |

**Phase 2: 服务增强**

| # | 任务 | 说明 |
|---|------|------|
| C-11 | 替换DemoVerificationCodeSender为真实SMTP邮件服务 | 新建EmailService实现 |
| C-12 | OCR服务接入后端API | 新建OCRController调用ocr-service |
| C-13 | 文件上传服务完善 | OSS/S3或本地存储策略 |
| C-14 | 数据库性能优化 | 索引审查/慢查询优化 |
| C-15 | 定时任务引擎功能完善 | 目前仅建表，需实现调度逻辑 |

**Phase 3: 硬件驱动层**

| # | 任务 | 说明 |
|---|------|------|
| C-16 | 小票打印机驱动（ESC/POS协议） | 虚拟打印→真实打印机 |
| C-17 | 扫码枪接口适配 | USB-HID输入处理 |
| C-18 | 厨房显示屏通信协议 | WebSocket推送或轮询 |
| C-19 | 电子秤数据读取 | 串口/RJ45重量采集 |

#### 启动指令（复制给AI）

```
你是后端与服务会话。负责Spring Boot API + OCR服务 + 数据库 + 硬件驱动层。

启动步骤：
1. 读取 PROJECT_PROGRESS.md 了解全局进度
2. 读取 .harness/PROTOCOL.md 了解工作协议
3. 读取 .harness/SESSION_ASSIGNMENT.md 了解你的任务清单
4. 加载相关 Skills:
   - .harness/skills/add-new-api.md（新增API时必读）
   - .harness/skills/fix-compilation-error.md（编译报错时参考）
   - .harness/skills/database-migration.md（数据库变更时参考）

工作范围（仅限以下目录）：
- backend/           → Spring Boot后端（主要工作区）
- ocr-service/       → OCR Python服务
- config/            → 生产环境配置
- SQL脚本文件        → 数据库迁移
- pom.xml            → 依赖管理

绝对禁止修改：
- frontend/ frontend-pos/ frontend-kitchen/ miniprogram/ → 由会话B负责
- PROJECT_PROGRESS.md → 由会话A负责
- .harness/          → 仅可通过 inbox 发消息

当前最高优先级（Phase 1）：
(1) 验证用户/角色/权限三个管理的CRUD API是否完整可用
(2) 编写PostgreSQL迁移脚本
(3) 配置生产环境profile
(4) Redis和JWT密钥外部化配置

必须遵循的后端规范：
- 逻辑删除使用 @TableLogic 注解，禁止物理删除
- 返回统一的 Result<T> 对象
- DTO接收请求参数，使用 @Valid 验证
- 实体类必须有 deleted 字段 + @TableLogic
- 查询不手动添加 deleted=0 条件
- Controller禁止直接注入Mapper
- 新增API必须按 add-new-api.md 清单执行

每完成一批改动运行 mvn compile 验证编译。
完成后通过 .harness/inbox/ 向其他会话发送完成通知。
```

---

## 四、文件所有权边界矩阵

```
路径                          会话A  会话B  会话C  说明
─────────────────────────────────────────────────────────
PROJECT_PROGRESS.md            🔒W     R      R     仅主控写入
.harness/                      🔒W     R      R     仅主控管理
.harness/inbox/                W      W      W     三方都可写消息
.harness/lock/session-lock.json 🔒W     R      R     仅主控管理

frontend/                       R     ★W      R     前端专属
frontend-pos/                   R     ★W      R     前端专属
frontend-kitchen/               R     ★W      R     前端专属
miniprogram/                    R     ★W      R     前端专属

backend/                        R      R     ★W     后端专属
ocr-service/                    R      R     ★W     后端专属
config/                         R      R     ★W     后端专属
pom.xml                         R      R     ★W     后端专属
*.sql                           R      R     ★W     后端专属

docs/                           R      R      R     只读参考
icons/ images/                  R      R      R     静态资源
scripts/ tools/                 R      R      R     工具脚本

图例: 🔒W=仅会话A写  ★W=专属写  W=可写  R=只读
```

---

## 五、协作时序图

### Phase 1: 管理后台闭环（当前阶段）

```
时间轴 →

[T=0min]  所有会话同时启动，各自读取上下文文件
           │
           ├─ A: 启动后端(mvn spring-boot:run) + 前端(npm run dev)
           ├─ B: 开始检查 UserManagement.vue 当前状态
           └─ C: 开始检查 UserController/Service/Mapper 完整性

[T=30min] 第一轮交付点
           │
           ├─ A: 完成11页冒烟测试，输出问题清单
           │   → 写入 inbox/to-b-xxx.json 和 inbox/to-c-xxx.json
           ├─ B: 完成用户管理+角色管理页面初版
           │   → inbox: "前端已准备好调用用户/角色API"
           └─ C: 完成用户/角色/权限 CRUD API验证
               → inbox: "以下API已确认可用..."

[T=60min] 第二轮交付点
           │
           ├─ A: 复测修复后的页面 + 更新进度文档到 v0.6.0
           ├─ B: 根据问题清单修复 + 完成剩余页面 + npm run build
           └─ C: 开始P1任务(PG迁移/prod配置/Redis)

[T=90min] 收尾
           │
           ├─ A: 全局审查 + 进度归档
           ├─ B: 最终 build 验证 + 进入pos端准备
           └─ C: mvn compile 最终验证 + PG迁移脚本交付
```

### Phase 2: 业务闭环（收银+后厨）

```
           ├─ A: 全链路模拟测试（下单→后厨→打印→追溯）
           ├─ B: pos端开发（下单/支付/打印UI）→ kitchen端（订单看板）
           └─ C: 订单API完善 → 厨房打印接口 → 硬件驱动(打印机/扫码枪)
```

### Phase 3: 配套完善

```
           ├─ A: 端到端验收 + 生产部署准备
           ├─ B: 小程序（扫码溯源/点餐）
           └─ C: OCR接入 + Redis真实连接 + 性能优化
```

---

## 六、消息箱使用规范

### 消息文件命名

```
.harness/inbox/{to}-{from}-{date}-{seq}.json
示例:
  to-session-c-20260405-001.json  → B发给C的消息
  to-session-b-20260405-003.json  → C发给B的第3条消息
  to-all-20260405-001.json        → 广播消息
```

### 消息类型

| type值 | 用途 | 示例场景 |
|--------|------|---------|
| `task-complete` | 任务完成通知 | "用户管理页面已完成" |
| `api-ready` | API就绪通知 | "以下API已验证通过" |
| `blocker` | 阻塞问题 | "需要先完成XXX才能继续" |
| `question` | 跨会话提问 | "这个字段含义是什么？" |
| `handoff` | 工作交接 | "我完成了X部分，Y部分需要你接手" |
| `progress` | 进度同步 | "已完成3/10任务" |

### 消息模板

```json
{
  "from": "session-b",
  "to": "session-c",
  "type": "api-ready",
  "timestamp": "2026-04-05T10:30:00",
  "title": "前端已完成用户管理页面，请确认API就绪",
  "content": {
    "apis_needed": [
      "GET /v1/users/page?page=1&size=10",
      "POST /v1/users",
      "PUT /v1/users/{id}",
      "DELETE /v1/users/{id}"
    ],
    "note": "页面已使用Element Plus Table + Dialog实现CRUD弹窗",
    "related_files": [
      "frontend/src/views/SystemSettings/PermissionCenter/UserManagement.vue",
      "frontend/src/api/user.ts"
    ]
  },
  "status": "unread"
}
```

---

## 七、硬件设备开发策略

### 开发阶段 vs 生产阶段

| 硬件类型 | 开发阶段策略 | 生产对接方式 | 负责会话 |
|---------|------------|------------|---------|
| **小票打印机** | 虚拟打印驱动（输出PDF预览/控制台日志） | ESC/POS指令集 + 58mm/80mm热敏纸 | C |
| **扫码枪** | 模拟键盘输入（大多数USB-HID即插即用） | 直接兼容 + 自定义前缀区分 | C→B |
| **厨房显示屏** | 浏览器模拟看板（大字体/自动刷新） | 部署到专用触控屏/Android盒子 | B→C |
| **电子秤** | 手动输入重量替代 | 串口(RS232)/RJ45读取重量数据 | C |
| **钱箱** | 软件模拟开启（日志记录） | RJ11接口脉冲信号 | C |

### 硬件驱动层建议架构

```
backend/src/main/java/com/example/demo/device/
├── printer/          打印机服务
│   ├── PrinterService.java        打印接口抽象
│   ├── EscPosPrinter.java         ESC/POS协议实现
│   └── MockPrinter.java           开发模式虚拟打印
├── scanner/          扫码枪服务
│   ├── ScannerService.java        扫码接口
│   └── UsbHidScanner.java         USB-HID处理
├── display/          显示屏服务
│   ├── DisplayService.java        显示接口
│   └── KitchenDisplay.java        厨房屏推送
└── scale/            电子秤服务
    ├── ScaleService.java          称重接口
    └── SerialScale.java           串口读取
```

---

## 八、风险控制

| 风险类型 | 场景 | 应对策略 |
|---------|------|---------|
| **文件冲突** | 两会话同时改同一文件 | 所有权边界明确，交叉文件由A协调 |
| **API不同步** | 前端调用的接口后端还没实现 | B先定义TS接口类型，C按契约实现；或C先定义DTO再B对接 |
| **DB变更影响** | C改了表结构导致B的查询出错 | C改表必须通过inbox通知B，暂停相关工作直到对齐 |
| **编译连锁失败** | 一处改动导致多处编译错误 | 各会话独立编译验证后再合并，A做最终集成验证 |
| **端口冲突** | 多个服务争抢同一端口 | 后端8081、前端3000、POS 3001、Kitchen 3002、OCR 8000 |
| **依赖不一致** | B/C装的npm/maven包版本不同 | 统一package-lock和pom锁定版本 |

---

## 九、快速参考

### 各服务端口

| 服务 | 开发端口 | 生产端口 | 启动命令 |
|------|---------|---------|---------|
| 后端API | 8081 | 8080 | `mvn spring-boot:run` |
| 管理后台前端 | 3000 | 80/443 | `npm run dev` |
| 收银端 | 3001 | — | `cd frontend-pos && npm run dev` |
| 后厨端 | 3002 | — | `cd frontend-kitchen && npm run dev` |
| OCR服务 | 8000 | 8000 | `cd ocr-service && python ocr_service.py` |
| PostgreSQL | 5432 | 5432 | 已安装在本机 |

### 关键命令速查

```powershell
# 启动全部服务（开发环境）
cd p:\my-new-project
start powershell { cd backend; mvn spring-boot:run }
start powershell { cd frontend; npm run dev }

# 编译验证
cd backend; mvn compile
cd frontend; npm run build

# 清理并重建
cd backend; mvn clean compile
cd frontend; rm -rf node_modules && npm install && npm run build
```

### 各会话首次启动Checklist

- [ ] 读取 PROJECT_PROGRESS.md
- [ ] 读取 .harness/PROTOCOL.md
- [ ] 读取 .harness/MULTI_SESSION.md
- [ ] 读取 .harness/SESSION_ASSIGNMENT.md（本文档）
- [ ] 加载相关的 Skill 模板
- [ ] 确认自己的文件所有权边界
- [ ] 建立 Todo 追踪列表
- [ ] 确认服务运行状态

---

## 十、版本历史

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-04-05 | v1.0.0 | 初始版本，三会话分派方案建立 |
