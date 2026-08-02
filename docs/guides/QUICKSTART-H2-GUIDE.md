# 🚀 快速启动指南 - H2开发环境测试

> **目标**: 使用H2内存数据库快速启动系统，测试登录和Demo展示  
> **适用场景**: 开发环境、Demo演示、无PostgreSQL/MySQL时  

---

## 一、环境准备检查清单

### 1.1 必需软件

| 软件 | 版本要求 | 检查命令 |
|-----|---------|---------|
| JDK | 21+ | `java -version` |
| Node.js | 18+ | `node -v` |
| npm | 9+ | `npm -v` |
| Maven | 3.9+ | `mvn -v` |

### 1.1.1 快速安装检查

```powershell
# 在PowerShell中执行
Write-Host "=== 环境检查 ===" -ForegroundColor Green
java -version
node -v
npm -v
mvn -version | Select-String "Apache Maven"
```

---

## 二、后端启动（H2模式）

### 2.1 启动方式一：Maven命令行（推荐）

```bash
cd p:\my-new-project\backend

# 使用H2 profile启动（开发模式）
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

**预期输出**:
```
===== 开始检查并初始化数据库 =====
当前活跃的Profile: h2
开发环境，初始化H2数据库表结构...
...
管理员用户创建成功，密码已加密: $2a$12$xxxxx
H2数据库表结构初始化完成
===== 数据库表初始化完成 =====

Started FoodTraceabilityApplication in x.x seconds
```

**验证启动成功**:
- 后端地址: http://localhost:8081/api
- H2控制台: http://localhost:8081/api/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - User: `sa`
  - Password: (空)

### 2.2 启动方式二：IDEA配置

1. 打开 `backend` 项目
2. 编辑 Run Configuration:
   - Main class: `com.example.demo.FoodTraceabilityApplication`
   - Active profiles: `h2`
   - VM options: `-Dspring.profiles.active=h2`
3. 点击运行

### 2.3 启动方式三：修改默认配置（永久切换）

如果希望默认就使用H2，编辑 [application.yml](file:///p:\my-new-project\backend\src\main\resources\application.yml):

```yaml
# 第19-42行，替换为:
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
    username: sa
    password:
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
  # ... 其他配置保持不变
```

---

## 三、前端启动

### 3.1 安装依赖（首次）

```bash
cd p:\my-new-project\frontend
npm install
```

### 3.2 启动开发服务器

```bash
npm run dev
```

**预期输出**:
```
VITE v5.0.0  ready in xxx ms

➜  Local:   http://localhost:3000/
➜  Network: http://192.168.x.x:3000/
```

---

## 四、登录测试

### 4.1 访问登录页

打开浏览器: **http://localhost:3000/login**

### 4.2 登录凭证

| 字段 | 值 |
|-----|---|
| 用户名 | `admin` |
| 密码 | `admin123` |

### 4.3 预期结果

✅ **成功**: 
- 跳转到首页 `/home` 或 Dashboard
- 显示"登录成功"提示
- 右上角显示用户信息

❌ **失败排查**:

#### 错误1: "Cannot read properties of undefined (reading 'metadata')"
- **原因**: 已修复（request.ts第480行）
- **解决**: 重启前端服务 `Ctrl+C` → `npm run dev`

#### 错误2: "网络错误" 或 "404"
- **原因**: 后端未启动或端口不对
- **解决**: 
  1. 确认后端已启动（看到"Started FoodTraceabilityApplication"）
  2. 检查端口: http://localhost:8081/api/actuator/health
  3. 前端API地址配置: 查看 `frontend/src/config/api.ts`

#### 错误3: "用户名或密码错误"
- **原因**: 数据库未初始化或密码不匹配
- **解决**:
  1. 访问H2控制台: http://localhost:8081/api/h2-console
  2. 执行SQL查询:
     ```sql
     SELECT id, username, password FROM users WHERE username = 'admin';
     ```
  3. 如果没有记录，重启后端（会重新初始化）

#### 错误4: "500 Internal Server Error"
- **原因**: 表结构不存在或字段缺失
- **查看日志**:
  ```bash
  # 后端控制台查找ERROR关键字
  # 或查看日志文件: backend/logs/food-traceability.log
  ```
- **常见问题**:
  - users表缺少某列 → DatabaseInitConfig会自动修复
  - 权限表为空 → 会自动插入默认数据

---

## 五、Demo样式参考页面

### 5.1 Prototype独立项目（推荐先看这个）

Prototype是一个**完整的可运行的Demo项目**，包含所有模块的样式参考：

#### 5.1.1 启动Prototype

```bash
cd p:\my-new-project\prototype

# 安装依赖（首次）
npm install

# 启动开发服务器
npm run dev
```

**访问**: http://localhost:8080

#### 5.1.2 Prototype包含的页面（30个）

| 模块 | 页面 | 路由 |
|-----|------|------|
| **Dashboard** | 总览面板 | `/` |
| **Dashboard** | 财务概览 | `/dashboard/finance` |
| **产品中心** | 产品列表 | `/product` |
| **产品中心** | 产品详情 | `/product/:id` |
| **订单中心** | 订单中心 | `/order` |
| **采购管理** | 采购列表 | `/purchase` |
| **采购管理** | 采购表单 | `/purchase/create` |
| **采购管理** | 采购详情 | `/purchase/:id` |
| **仓储管理** | 库存总览 | `/warehouse` |
| **仓储管理** | 库存列表 | `/warehouse/inventory` |
| **仓储管理** | 入库管理 | `/warehouse/inbound` |
| **营销增长** | 营销概览 | `/marketing` |
| **营销增长** | 优惠券管理 | `/marketing/coupon` |
| **财务中心** | 发票列表 | `/finance/invoice` |
| **财务中心** | 账本管理 | `/finance/ledger` |
| **财务中心** | 凭证列表 | `/finance/voucher` |
| **人事管理** | 考勤管理 | `/hr/attendance` |
| **人事管理** | 薪酬管理 | `/hr/salary` |
| **员工管理** | 员工列表 | `/employee` |
| **食品追溯** | 追溯链路 | `/traceability` |
| **数据决策** | 销售分析 | `/data-decision/sales` |
| **数据** | 样例数据 | `/data/sample` |
| **系统设置** | 设置首页 | `/settings` |
| **系统设置** | 基础设置 | `/settings/basic` |
| **系统设置** | 模块管理 | `/settings/module` |
| **系统设置** | 用户管理 | `/settings/user` |
| **系统设置** | 角色权限 | `/settings/role` |
| **系统设置** | 操作日志 | `/settings/log` |
| **系统初始化** | 向导 | `/wizard` |
| **个人中心** | 个人信息 | `/profile` |
| **登录页** | 登录 | `/login` |

#### 5.1.3 Prototype的CSS架构（5层体系）

```
styles/theme/
├── index.css          # 入口文件（按顺序导入）
├── _base.css          # 第1层：全局变量、重置样式
├── _themes.css        # 第2层：11套主题定义
├── _components.css    # 第3层：组件适配样式
├── _layout.css        # 第4层：布局样式
└── _dark.css          # 第5层：深色模式覆盖
```

**主题列表** (在 `_themes.css` 中定义):
1. 琥珀厨房 (amberKitchen)
2. 翡翠餐厅 (emeraldRestaurant)
3. 烘焙工坊 (bakery)
4. 陶土餐厅 (terracotta)
5. 暗黑办公 (darkOffice)
6. 商务行政 (business)
7. ... (共15套，含深色变体)

#### 5.1.4 如何参考Prototype的样式？

**方法1: 直接运行对比**
```bash
# 终端1：启动主项目前端
cd frontend && npm run dev  # http://localhost:3000

# 终端2：启动Prototype
cd prototype && npm run dev  # http://localhost:8080

# 浏览器并排对比两个窗口
```

**方法2: 查看构建后的文件**
```bash
# Prototype已经构建好了，可以直接打开dist/index.html
cd prototype/dist
# 用任意HTTP服务器打开，或直接双击index.html（部分功能受限）
```

**方法3: 复制组件代码**
从 `prototype/src/views/` 复制需要的组件到 `frontend/src/views/`

---

### 5.2 StyleReference页面（主项目中）

如果你想在主项目中添加一个样式参考页面：

#### 5.2.1 添加路由配置

编辑 [frontend/src/router/constant-routes.ts](file:///p:\my-new-project\frontend\src\router\constant-routes.ts) 或对应的路由文件：

```typescript
{
  path: '/style-reference',
  component: () => import('@/views/StyleReference.vue'),
  meta: {
    title: '样式参考',
    icon: 'Brush',
    requiresAuth: false  // 不需要登录即可访问
  }
}
```

#### 5.2.2 访问地址

启动后访问: **http://localhost:3000/style-reference**

---

## 六、H2数据库常用操作

### 6.1 连接H2控制台

1. 确保后端正在运行
2. 打开: http://localhost:8081/api/h2-console
3. 配置连接:
   - JDBC URL: `jdbc:h2:mem:testdb`
   - User Name: `sa`
   - Password: (留空)
4. 点击 "Connect"

### 6.2 常用查询SQL

```sql
-- 1. 查看所有表
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = 'PUBLIC';

-- 2. 查看管理员用户
SELECT id, username, name, status, roles FROM users WHERE username = 'admin';

-- 3. 查看角色列表
SELECT * FROM roles;

-- 4. 查看权限列表
SELECT * FROM permissions LIMIT 20;

-- 5. 手动重置admin密码（如果需要）
UPDATE users SET password = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.VTtYA.qGZvKG6G' 
WHERE username = 'admin';
-- 注意：这个哈希对应的是 Admin@123，不是 admin123

-- 6. 查看表结构
DESCRIBE users;
-- 或
SELECT COLUMN_NAME, TYPE_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'USERS';
```

### 6.3 导出/导入数据（可选）

```sql
-- 导出为SQL脚本（在H2 Console中执行）
SCRIPT TO 'C:/temp/backup.sql';

-- 从SQL脚本导入
RUNSCRIPT FROM 'C:/temp/backup.sql';
```

---

## 七、常见问题FAQ

### Q1: 为什么选择H2而不是MySQL/PostgreSQL？

**A**: 
- ✅ **零配置**: 无需安装任何数据库软件
- ✅ **快速启动**: 内存数据库，启动速度极快
- ✅ **自动初始化**: 每次启动自动建表和插入测试数据
- ✅ **适合开发**: 完美适合开发和Demo场景
- ⚠️ **限制**: 数据不持久化（重启丢失），不适合生产环境

### Q2: H2的数据会保存吗？

**A**: 
- 当前配置是**内存模式** (`jdbc:h2:mem:testdb`)
- 应用关闭后数据**全部丢失**
- 如需持久化，改为文件模式:
  ```yaml
  url: jdbc:h2:file:./data/testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
  ```

### Q3: 如何切换到PostgreSQL？

**A** (后续迁移时):
1. 安装PostgreSQL 18
2. 修改 `application-prod.yml` 的datasource配置
3. 执行PostgreSQL迁移脚本: `sql/postgres-schema-v0.12.sql`
4. 使用profile启动: `-Dspring.profiles.active=prod`

### Q4: 登录后页面空白或报错？

**A**: 
1. 按F12打开开发者工具
2. 查看Console标签的错误信息
3. 查看Network标签的API请求状态
4. 常见原因:
   - API返回500 → 查看后端日志
   - 401未授权 → Token过期，刷新页面
   - 404路由不存在 → 检查路由配置

### Q5: 如何重置数据库？

**A**: 
直接重启后端即可！H2内存模式每次启动都会重新初始化。

如需清理特定表：
```sql
-- 在H2 Console中执行
DELETE FROM users;
DELETE FROM roles;
DELETE FROM orders;
-- ... 其他表
```

然后重启后端，会重新插入默认数据。

---

## 八、性能优化建议（可选）

### 8.1 加快启动速度

如果觉得启动慢，可以禁用不必要的功能：

编辑 [application-h2.yml](file:///p:\my-new-project\backend\src\main\resources\application-h2.yml):

```yaml
logging:
  level:
    root: WARN  # 降低日志级别
    com.example.demo: INFO
    
spring:
  jpa:
    show-sql: false  # 关闭SQL日志
```

### 8.2 增加JVM内存

如果遇到OOM（内存不足）：

```bash
set JAVA_OPTS=-Xmx1024m -Xms512m
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

---

## 九、下一步工作建议

### 9.1 立即可以做的（今天）

- [x] 使用H2启动后端
- [x] 测试 admin/admin123 登录
- [x] 启动Prototype查看样式参考
- [ ] 对比主项目和Prototype的样式差异
- [ ] 记录需要统一的样式问题

### 9.2 本周内完成的

- [ ] 将Prototype的优秀样式迁移到主项目
- [ ] 修复发现的前端页面显示问题
- [ ] 完善至少3个核心模块的CRUD功能测试
- [ ] 编写基本的自动化测试用例

### 9.3 下周计划

- [ ] PostgreSQL迁移准备（评估工作量）
- [ ] 收银端功能回归测试
- [ ] 后厨端打印功能测试
- [ ] 权限系统端到端测试

---

## 十、技术支持

### 10.1 关键文件位置

| 文件 | 路径 | 说明 |
|-----|------|------|
| H2配置 | `backend/src/main/resources/application-h2.yml` | H2数据库配置 |
| 数据库初始化 | `backend/src/main/java/.../DatabaseInitConfig.java` | 自动建表和数据初始化 |
| 密码工具 | `backend/src/main/java/.../security/utils/PasswordUtils.java` | BCrypt加密 |
| Admin用户初始化 | `DatabaseInitConfig.insertDefaultAdminUser()` | 第2239行 |
| 前端API配置 | `frontend/src/config/api.ts` | 后端API地址 |
| 登录逻辑 | `frontend/src/stores/auth.ts` | login()方法 |
| Prototype入口 | `prototype/src/main.ts` | Demo项目入口 |
| Prototype路由 | `prototype/src/router/index.ts` | Demo路由配置 |

### 10.2 日志位置

- 控制台日志: 直接在终端查看
- 文件日志: `backend/logs/food-traceability.log` (如果配置了)
- H2日志: 同终端输出

---

## 十一、成功标志

当以下所有项都通过时，说明环境搭建成功：

- [ ] 后端启动无ERROR（WARN可以接受）
- [ ] 访问 http://localhost:8081/api/actuator/health 返回 `{"status":"UP"}`
- [ ] H2控制台可以连接并查询users表
- [ ] 前端启动无编译错误
- [ ] 使用 admin/admin123 可以成功登录
- [ ] 登录后可以看到Dashboard页面
- [ ] Prototype可以在 http://localhost:8080 正常访问
- [ ] 至少可以浏览Prototype中的5个不同模块页面

---

**🎉 恭喜！你现在可以开始开发和测试了！**

如有问题，请查看上方FAQ或联系技术支持。
