# 多会话并行工作协议 (Multi-Session Coordination)

> 本文档是 `.harness/PROTOCOL.md` 的补充协议
> 解决多个 Trae 会话窗口同时工作时如何不冲突、可交流

---

## 一、架构概览

```
┌─────────────────────────────────────────────┐
│           共享磁盘 (p:\my-new-project)        │
│                                             │
│  ┌──────────────┐  ┌───────────────────┐   │
│  │ SESSION-LOCK │  │ PROJECT_PROGRESS │   │
│  │ .harness/lock/│  │ .md              │   │
│  │ (会话锁)      │  │ (共享记忆)       │   │
│  └──────────────┘  └───────────────────┘   │
│                                             │
│  ┌──────────────────────────────────────┐    │
│  │ SESSION-INBOX (.harness/inbox/)     │    │
│  │ ├── session-A-msg-001.json         │    │
│  │ ├── session-B-msg-001.json         │    │
│  │ └── ...                            │    │
│  └──────────────────────────────────────┘    │
└─────────────────────────────────────────────┘

会话A (窗口1)          会话B (窗口2)          会话C (窗口3)
    │                    │                    │
    ├─ 读锁 → 获得权限   ├─ 读锁 → 等待/换区   ├─ 读锁 → 等待/换区
    ├─ 写 Progress.md    ├─ 写 Progress.md     ├─ 写 Inbox 消息
    ├─ 读其他会话消息     ├─ 读其他会话消息      ├─ 执行分配的任务
    └─ 完成后释放锁       └─ 完成后释放锁         └─ 完成后释放锁
```

---

## 二、会话锁机制（Session Lock）

### 2.1 锁文件结构
```
目录: .harness/lock/

文件: session-lock.json (全局唯一)
内容:
{
  "sessionId": "session-A",
  "lockedAt": "2026-04-05T02:00:00",
  "claimedModules": ["auth", "login-fix"],
  "ttl": 3600,
  "status": "active"
}
```

### 2.2 锁操作规则

#### 获取写锁（修改关键文件前必须获取）
```json
// 请求格式
{
  "action": "acquire",
  "sessionId": "session-B",
  "targetFiles": ["router/index.ts", "stores/auth.ts"],
  "reason": "重构认证流程"
}

// 成功响应
{ "status": "granted", "message": "锁已获得，请在3600s内完成" }

// 失败响应
{ "status": "denied", "holder": "session-A", "waitTime": 1200 }
```

#### 锁的规则
```
✅ 允许并发读: 多个会话可以同时读取同一文件
❌ 禁止并发写: 同一时刻只有一个会话能修改特定文件
⚠️ 自动过期: TTL超时(默认1小时)后自动释放
🔄 强制释放: 锁持有者完成后主动释放
```

### 2.3 文件归属分区（推荐方式）
```
为避免频繁抢锁，建议按模块预先分配:

┌────────────────┬───────────────┬──────────────┐
│ 模块          │ 默认负责会话  │ 可临时借用    │
├────────────────┼───────────────┼──────────────┤
│ 认证/Auth     │ 会话-A        │ 会话B(需通知) │
│ 系统/设置     │ 会话-B        │ 会话A(需通知) │
│ 财务/Finance  │ 会话-C        │ 会话A(需通知) │
│ 食品追溯      │ 会话-A        │ 会话B(需通知) │
│ 基础设施/DB   │ 仅主会话      │ 其他只读     │
│ PROJECT_PROGRESS│ 所有会话可写 │ 最后写入胜出  │
│ .harness/     │ 仅主会话      │ 其他只读     │
└────────────────┴───────────────┴──────────────┘
```

---

## 三、消息通信（Session Inbox）

### 3.1 消息格式
```json
{
  "id": "msg-001",
  "from": "session-A",
  "to": "session-B",           // all = 广播给所有会话
  "type": "progress" | "warning" | "question" | "blocker-release",
  "subject": "登录修复已完成",
  "body": "AnomalyAccessLogFilter已修复copyBodyToResponse，请刷新前端验证",
  "timestamp": "2026-04-05T02:00:00",
  "readBy": ["session-C"]           // 已读会话列表
}
```

### 3.2 消息类型说明

| type | 使用场景 | 示例 |
|------|---------|------|
| `progress` | 报告工作进展 | "我完成了登录修复，你们可以测试了" |
| `warning` | 发现可能影响他人的问题 | "我改了SecurityConfig，你们的认证可能受影响" |
| `question` | 向其他会话提问 | "有人知道财务模块的Voucher实体在哪吗？" |
| `blocker-release` | 释放文件锁 + 通知 | "我释放了router/index.ts的锁" |
| `handover` | 任务交接 | "API接口部分我做完了，前端对接交给你们" |
| `request-block` | 请求锁定文件 | "我要改auth.ts，谁在用吗？" |

### 3.3 消息生命周期
```
创建 → 写入 .harness/inbox/{from}-{id}.json
广播 → 写入 .harness/inbox/all-{id}.json
读取 → 扫描 inbox/ 目录, 过滤 to=self 或 to=all
已读 → 在 readBy 数组中添加自己的 sessionId
清理 → 已被所有活跃会话读取的消息可定期清理(>24h)
```

---

## 四、标准工作流（多会话版）

### 4.1 会话启动检查清单
```
每个新会话窗口打开时:

1. [ ] 分配会话ID: session-{A/B/C/D...} (或时间戳)
2. [ ] 读取 PROJECT_PROGRESS.md 了解当前状态
3. [ ] 扫描 .harness/lock/session-lock.json 查看当前锁状态
4. [ ] 扫描 .harness/inbox/ 查看是否有给自己的消息
5. [ ] 确认自己负责的模块区域
6. [ ] 创建 Todo 列表
```

### 4.2 工作中检查清单（每10分钟）
```
1. [ ] 锁是否即将过期? (剩余<10min则续期或尽快完成)
2. [ ] inbox是否有新消息? (特别是 warning 类型)
3. [ ] 我是否要修改他人负责区域的文件? (需要发 request-block)
4. [ ] 当前进度是否值得写入一条 progress 消息?
```

### 4.3 工作结束检查清单
```
1. [ ] 编译通过 (mvn compile / npm run build)
2. [ ] 释放所有持有的锁
3. [ ] 发送 progress 或 handover 消息汇报结果
4. [ ] 更新 PROJECT_PROGRESS.md (注意并发写入)
5. [ ] 清理临时文件
```

---

## 五、冲突解决策略

### 场景1: 两个会话都要改同一个文件
```
优先级判断:
- P0紧急修复 > P1功能开发
- 先到者得之, 后到者等待或协商拆分
- 可将文件拆分为不同函数/方法, 各改各的

协商示例(session-B发现session-A锁了auth.ts):
  session-B 发送: { type:"request-block", body:"我需要修复AuthController的一个bug,预计5min" }
  session-A 回复: { type:"progress", body:"我快好了,还有3min,你先处理别的或者等我" }
  或者: session-A 释放部分锁: { type:"blocker-release", files:["AuthController.java"], keep:["AuthService.java"] }
```

### 场景2: 进度文档并发写入
```
策略: 最后写入胜出(Last Write Wins)
缓解:
- 不同会话更新不同章节 (session-A更版本历史, session-B更待办事项)
- 合并前先读取最新版本
- 重要变更用 inbox 消息通知而非直接覆盖
```

### 场景3: 一个会话的修改破坏了另一个会话的工作
```
预防:
- 修改公共文件前发 warning 消息
- 改接口签名前确认无人依赖旧签名

补救:
- inbox 消息告知具体改动
- 受影响会话决定是否 rebase 自己的修改
```

---

## 六、推荐的多会话分工模式

### 模式1: 前后端分离（最常用）
```
┌─ Session-A (后端专注)
│  ├── 后端编译错误修复
│  ├── 新增 API 接口
│  ├── 数据库迁移
│  └── 认证体系优化
│
├─ Session-B (前端专注)
│  ├── 前端页面开发
│  ├── 组件拆分
│  ├── 路由配置
│  └── 样式调整
│
└─ Session-C (测试+文档)
    ├── 功能冒烟测试
    ├── 进度文档维护
    ├── Skill模板完善
    └── 跨会话协调
```

**接触点**: api/types定义、路由路径、字段命名 — 这些需要通过 inbox 协调。

### 模式2: 功能模块分离
```
┌─ Session-A → 认证 + 用户 + 权限模块
├─ Session-B → 财务 + 报表 + 税务模块  
└─ Session-C → 食品追溯 + 库存 + 采购模块
```

**接触点**: 公共组件、stores、utils、基础配置 — 尽量减少跨模块依赖。

### 模式3: 主从模式（推荐初期使用）
```
┌─ Session-Master (主会话)
│  ├── 架构决策
│  ├── 关键文件修改 (PROTOCOL.md, router/index.ts, SecurityConfig)
│  ├── 进度文档最终版本
│  └── 跨会话冲突仲裁
│
└─ Session-Worker-N (工作会话, 可多个)
    ├── 接收主会话分配的具体任务
    ├── 只在分配的范围内工作
    ├── 完成后 handover 给主会话
    └── 不修改任何未授权的文件
```

---

## 七、快速启动命令参考

### 创建新会话
```
1. 打开新的 Trae 窗口
2. 设定会话名: export SESSION_ID="session-D"
3. 运行启动检查:
   cat .harness/lock/session-lock.json  # 检查锁状态
   ls .harness/inbox/*.json             # 检查新消息
4. 开始工作
```

### 发送消息
```bash
# 写入消息文件
cat > .harness/inbox/session-D-msg-001.json << 'EOF'
{
  "id": "msg-001",
  "from": "session-D",
  "to": "all",
  "type": "progress",
  "subject": "数据库初始化脚本完成",
  "body": "scheduled_task等缺失表已补全, 后端重启正常",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "readBy": []
}
EOF
```

### 请求文件锁
```bash
# 查看当前锁
cat .harness/lock/session-lock.json

# 获取锁 (手动或让Agent执行)
# Agent会在修改关键文件前自动检查和获取锁
```

---

## 八、与 ClawTeam 的对比

| 维度 | 我们的方案 | ClawTeam |
|------|-----------|----------|
| 通信方式 | JSON 文件 inbox | CLI inbox 命令 |
| 协调粒度 | 文件级别 | 任务级别 |
| 隔离方式 | 手动约定 + 锁文件 | git worktree + tmux |
| 适用规模 | 2-5个会话 | 8+ Agent |
| 复杂度 | 低 (无需额外服务) | 中 (需tmux+git配置) |
| 适合场景 | Trae IDE 多窗口 | CLI Agent Swarm |

**我们的方案更适合当前环境**——轻量、无依赖、基于已有文件系统。
