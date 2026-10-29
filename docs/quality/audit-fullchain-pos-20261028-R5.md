# R5 终验通过报告 — 准予联调

> **产物编号**：AUDIT-FULLCHAIN-POS-20261028-R5
> **性质**：只读穿透终验。不采信开发自测汇报，全部以抓包、前端 a11y/DOM、SQL 实测为准。
> **审计时间**：2026-10-29 12:52–13:05
> **范围**：仅复测 R4 打回的两项（P1-UI-REFUND-026 + P2-POS-AUTH-023）及必要的对照回归，其余 5 项沿用 R4 已验收结论，不重测全量。

---

## 0. 判定总表

| # | 验收项 | 判定 | 关键证据 |
|---|---|---|---|
| 1 | 接口声明与执行一致性（P2-POS-AUTH-023） | ✅ **PASS** | cashier permissions 46→**43**，三个退款动作键（order:refund / order:refund:initiate / refund:manage）**物理删除**；不存在的订单号退款 → **403 先于参数校验**；admin 退款回归 200 全链无损 |
| 2 | 前端 UI 彻底关闭（P1-UI-REFUND-026） | ✅ **PASS** | cashier：列表 DOM / 详情 a11y / 全文档 DOM / 首页 **退款按钮全部 = 0**；admin 对照：详情 a11y ×2 + 列表行 ×1；且排除运行时残留假象（denied/granted = null） |

**最终判定：【PASS】— 《R5 终验通过：准予联调》。** R1→R4 六轮累积的全部 P1/P2 打回项清零（遗留仅 2 项 P3 观察项，见 §4），发布门禁 FAIL=0。

---

## 1. 环境核验（多疑探针实录）

| 项 | 开发汇报 | 实测 | 判定 |
|---|---|---|---|
| 进程 | PID 18548 | **实际服务进程 PID 24076**（JVM 12:54:31，:8081 LISTENING，HTTP 200）；另有 mvnd 启动器 9800（父子对，正常） | 汇报口径过时（不阻塞，见 §5 备注） |
| 类编译 | 12:24:05 | PosOrderPaymentServiceImpl / PosAuthServiceImpl.class = **12:24:04** < JVM 12:54:31 ✓ 部署时序健康 | ✓ |
| Flyway | V20261029_002 已执行 | `flyway_schema_history`: **20261029.002 success=t installed 12:24:31**；20261029.001 success=t 02:51:39 | ✓ |
| 前端 | — | frontend-pos/src 于 03:20 后**零改动**——路径 A（纯数据修复、前端闸门代码不动）与设计一致 | ✓ |

**迁移内容审查**（`V20261029_002__revoke_cashier_refund_permissions.sql`）：
- 硬 DELETE（非 deleted=1 软删——与「声明↔执行一致」的统计口径匹配）ROLE_CASHIER 的三个退款动作键；
- 内置断言块：若有残留退款发起权限 → 迁移报错；若 refund:view 被误删 → 迁移报错（只读查看码刻意保留）；
- 幂等（DELETE 语义天然可重复执行）。

---

## 2. Test 1：接口声明与执行一致性 — ✅ PASS

### 2a 声明侧撤回（三方对账）

| 层 | 证据 | 结论 |
|---|---|---|
| DB 角色映射 | `role_permissions ⋈ permissions`：ROLE_CASHIER 退款键仅剩 `refund:view`；三个动作键 **0 行** | ✓ 撤回到位 |
| **DB direct 授权旁路** | `user_permissions`（vcashier01=2080170081205395463）：退款键 **0 行**（direct 计权=0） | ✓ 无旁路（PosAuthServiceImpl:576 读取的第二数据源已排除） |
| 登录响应 | cashier fresh login permCount=**43**（R4 记录 46），refund 键 = `['refund:view']`；`product:food:view`/`pos:menu:query`/`pos:order:query` **零误伤** | ✓ 46→43 恰为三键，无附带损伤 |
| admin 对照 | permissions = `['*']` 未变 | ✓ |

### 2b 执行侧拦截

```
[1] cashier POST /v1/pos/orders/order/NO-SUCH-T999/refund（不存在的订单号）
    → HTTP 200 body code=403「权限不足，无法访问该资源」
    → 权限闸先于参数/业务校验（未出现 404「订单不存在」）✓（越权探测不泄漏订单存在性）

[2] cashier POST .../order/T20261029007/refund（真实已支付 ¥18 现金单）
    → code=403；订单原样（payment/refund 状态零变化）✓

[3] admin POST .../order/T20261029007/refund（回归对照）
    → code=0 ✓
      foods.stock id2: 867 → 868（回补 +1）✓
      orders: order_status=5, refund_amount=1800 ✓
      fund_flows: FF202610290023, direction=2, amount=1800 ✓（冲账流水在）
    → admin 退款能力未被撤回操作波及
```

**一致性结论**：cashier = 无声明 + 端点 403（对齐）；admin = `['*']` + 端点 200（对齐）。两端点行为与权威数据源一一对应，P2-POS-AUTH-023 关闭。

---

## 3. Test 2：前端 UI 彻底关闭 — ✅ PASS

### 3a 收银员（vcashier01，UI 登录，a11y 身份确认「测试收银员 工号 vcashier01」）

| 断言面 | 方法 | 结果 |
|---|---|---|
| 订单查询列表行 | DOM 全量扫描 `button ∋ 退款` | **[] 零个** |
| T20261028002（paid 未退）详情弹窗 | a11y 树 `[button] *退款*` | **[] 零个** |
| 详情弹窗（DOM 复核） | `document.querySelectorAll('button')` 过滤 | **[] 零个** |
| 详情弹窗标题区 | a11y：`[generic] "关闭"`（R4 为 `"菜品明细 关闭 申请退款"`） | 「申请退款」字样消失 |
| 首页/菜单面 | DOM 扫描 | **0 个**退款按钮 |

### 3b 店长/管理员对照（admin 同单同页面）

| 断言面 | 结果 |
|---|---|
| 详情弹窗 a11y `[button] *退款*` | **×2**（@e20518/@e20529；标题区「申请退款」+ 底部）|
| 详情弹窗 DOM | `["申请退款","退款"]`（后者为身后列表行的行级退款入口） |
| 列表行 | 行级「退款」按钮 ×1 |
| 弹窗标题区 | `[generic] "关闭 申请退款"`（恢复 R4 形态） |

**对照结论：cashier 0 / admin ≥2，严格反差成立。且收银员对「已支付单」的行级与详情级退款入口同时隐没。**

### 3c 假阳性排除（本报告的关键审计动作）

按钮为 0 可能有两种成因：正确的「声明撤回」与错误的「`pos-perm-denied` 运行时残留」。实测：

```
cashier 全新 localStorage（clear）+ fresh login 后：
  pos-perm-denied  = null
  pos-perm-granted = null
  pos-user.permissions = 43 项（refundKeys = ['refund:view']）
→ 零残留，0 按钮完全由「声明撤回 → 权限码闸门关闭」所致（usePermission.ts evaluate():
  permissions 无 order:refund、roles 非管理集、granted/denied 皆空 → allowRefund=false）
```

---

## 4. 遗留清单（不阻塞放行）

| 编号 | 等级 | 内容 | 处置建议 |
|---|---|---|---|
| P3-POS-AUTH-027 | P3 | `refund:view` 仍向 cashier 声明，但后端无任何 `@PreAuthorize` 端点消费该码（仅 AdminPermissions.java:145 目录清单）；属「悬空只读声明」 | 后续权限目录治理时统一裁决（保留为预留只读码 or 撤销）；因前端退款闸门绑定 `order:refund`（非 refund:view），不影响 UI 行为 |
| P3-POS-DOC-028 | P3 | 部署通报口径与实际不符（报 PID 18548 / 类 12:24:05，实际服务进程 24076、JVM 12:54:31；Flyway 12:24:31 由首次启动实例执行） | 流程改善项：部署通报需以「最新探针」为准 |
| 数据卫生 | 备注 | R3 打回时曾记录 active 班次达 36 条的开发测试残留，本轮未新增班次（UI 均走「稍后」），R5 全程零班次变动 | 后续可一次性归档历史测试班次 |

**Release Gate：FAIL = 0 → 准予放行。**

---

## 5. 结论

**【PASS】— 《R5 终验通过报告：准予联调》。**

六轮（R1 全链路审计 → R2 回归 → R3 穿透 → R4 终验 → R5 前置核查 + 终验）全部 P1/P2 缺陷闭环：

- 资金口径（closing_cash 钱箱公式）✓、退款全链（库存回补/KO cancelled/锁料释放/冲账流水/cancel_time）✓
- 服务端定价与回显消毒 ✓、售罄拦截零副作用 ✓、交易号口径 ✓
- 开班/交班防越权（身份绑定 + 终端隔离）✓、role_id 数据规范化 ✓
- 权限三方对齐（声明 ↔ 前端闸门 ↔ 端点校验）：收银员退款动作权限撤回 + UI 全入口关闭 + 端点 403 兜底，且能力保留于管理侧 ✓

系统具备生产级业务流转能力，**准予 POS 前端（:3001）与后端（:8081）进入联调**。联调期间建议保留本报告 §4 的 P3 观察项跟踪。

## 6. 取证索引

- token：`tmp/pos-audit-r5-ca.token`（cashier, AUDIT-R5-CA）/ `tmp/audit-r5-ad.token`（admin, AUDIT-R5-AD）
- 订单：T20261029007（R5 回归退款单：创建→支付→cashier 403→admin 退款闭环）；T20261028002（UI 对照样本，保持 paid 未退）
- 迁移：`backend/src/main/resources/db/migration/V20261029_002__revoke_cashier_refund_permissions.sql`（success @ 12:24:31）
- 关键代码锚点：`PosOrderPaymentServiceImpl.java:332`（端点角色闸）、`PosAuthServiceImpl.java:558-588`（权限装配）、`UserMapper.xml:8-33`（两路权限 SQL）、`usePermission.ts`（前端权限码闸门）
- 关联：R1/R2/R3/R4 报告（`docs/quality/audit-fullchain-pos-20261028*.md`，R4 附录 A 为本轮修复指令源头）
