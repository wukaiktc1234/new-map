# 第十六轮：审计三项（退款按钮 / 后厨状态 / 菜单 403）根因修复

> 三条都是**数据没对齐**导致的前端错乱，已定位到具体行并修复。新增 9 项断言，回归 42/30/25/16/15/12/5 全绿。

---

## 1. admin 也看不到退款按钮

**根因（不是条件写反，是时机错了）**：
`usePermission` 里的权限快照 `snapshot` 是**模块加载时**初始化的：

```ts
const snapshot = ref<Snapshot>(readSnapshot())   // ← import 时就读 localStorage
```

而 `usePermission` 会在 `Home.vue` 顶层被 import —— 那时用户**还没登录**（`pos-user` 为空或上一个账号），
快照就是空权限；登录成功后**没有任何地方刷新它** → `canRefund` 永远 false → 所有人（含 admin）都看不到按钮。

**修法**：
1. `Login.vue` 登录成功后立刻 `refreshCapabilities()`；
2. `OrderQueryDialog` / `OrderDetailDialog` 打开时再刷新一次（`watch(modelValue, { immediate: true })`）；
3. 监听 `storage` 事件（另一个标签页登录/授权时同步）；
4. `usePermission` 增加内置 admin 兜底：`employeeId === 'admin'` 直接放行——
   与后端 `PosAuthServiceImpl` 里"admin 特判"保持同一口径，避免后端未回显 roles 时把超管也挡住。

实测：admin（roles:['admin'], permissions:['*']）→ 退款按钮 1 个；店长角色 → 1 个；
收银员 `ROLE_CASHIER` → 0 个（这是**应有**行为）；后端回 403 → 立即隐藏。

## 2. 后厨状态失真（served 显示待处理、refunded 显示待制作）

**根因**：映射表和后端码值不一致。后端 `OrderItemNew` 的注释写得很清楚：

```
厨房状态: 0 待制作 / 1 制作中 / 2 已完成 / 3 已上菜 / 4 已退款
```

而前端 `KITCHEN_STATUS` 里写的是 `'3': 已取消`、**根本没有 4**，于是：
- 已上菜（3）→ 被翻译成"已取消"；
- 已退款（4）→ 查不到 → 落回默认值"待制作"；
- 后端偶尔不返回该字段时，默认文案又是"待处理"。

**修法**：
1. 修正数字映射：`0 待制作 / 1 制作中 / 2 已完成 / 3 已上菜 / 4 已退款 / 5 已取消`；
   文本码补齐 `served / delivered / refunding / refunded / partial_refunded`；
2. 新增 `resolveKitchenStatus(order)`，按订单上下文做兜底（优先级从高到低）：
   1. 订单已退款/取消 → 后厨直接跟随（**绝不可能还在"待制作"**）
   2. 后端给了 `kitchenStatusName`（中文名）→ 直接用（真相在后端）
   3. 有码值 → 按表翻译
   4. 都没有 → 按订单状态推断（已完成→已上菜，其余→待制作）
3. 空值文案由"待处理"改为"未进入后厨"（更真实，不再误导）。

实测四条样例全部正确：
```
kitchen=3 订单=2 → 已上菜   ✅
kitchen=4 订单=5 → 已退款   ✅
kitchen=0 订单=5 → 已退款   ✅（订单已退款，后厨不再显示待制作）
kitchen=1 订单=1 → 制作中   ✅
```

## 3. 菜单 403：把"像坏了"改成"没权限"

**根因**：`.load-error` 只有一行红字 + 一个小按钮，红色感叹号 + "权限不足，点击重试"，
收银员的第一直觉是"系统故障"。

**修法**：
1. `useMenu` 记录 HTTP 状态码 `loadErrorStatus`（403 与其它错误分开处理），
   403 文案改为「当前账号没有查看商品的权限（403）」；
2. `MenuSection` 新增 403 专属空态：
   - **锁形插画**（amber 中性色——这是权限问题，不是故障，不用红色）
   - 标题「账号暂无商品查看权限」
   - 说明「当前登录账号缺少商品查看权限（403）。请联系店长开通后重试；也可以先让店长在后台确认该角色的『商品 / 分类 / 套餐』查看权限。」
   - 主操作「重新加载」+ 辅助提示「权限变更后无需重启，点一次重新加载即可」
3. 非 403 的错误仍保留断线插画 + 危险色，文案取自后端 message。

实测：插画渲染 ✅、文案含"暂无商品查看权限"与"403" ✅、重试入口存在 ✅。

---

## 验证汇总

| 套件 | 项数 | 结果 |
|---|---|---|
| pos-round16-verify.cjs（新增） | 9 | 全绿 |
| pos-round13-verify.cjs（权限） | 15 | 全绿 |
| pos-round2-verify.cjs（主链路） | 42 | 全绿 |
| pos-retrieve-verify.cjs | 30 | 全绿 |
| pos-round12-verify.cjs | 25 | 全绿 |
| pos-visual-verify.cjs | 16 | 全绿 |
| pos-round15-verify.cjs | 12 | 全绿 |
| pos-negative-cash.cjs | 5 | 全绿 |

截图：round16-menu-403-dark.png（403 空态，深色）。

## 待后端配合

- `/v1/pos/auth/login` 需要把 `roles` / `permissions` 回显到 `userInfo`（后端改动已提交，需重新部署）；
  未部署前前端靠"admin 特判 + 403 记忆"兜底，店长仍可能看不到退款入口。
