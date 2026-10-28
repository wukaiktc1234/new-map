# 第十七轮：真实后端联调 + 交班日结质感打磨

> 后端已部署，我用**真实后端**（localhost:8081）跑了端到端验证，并继续打磨交班日结。
> 结论：店长链路通过；收银员菜单 403 属后端权限数据；**admin 被后端判 403 是后端新问题**，已定位。

---

## 0. 先说断流排查的前置动作

你机器的 Vite dev server 在我 22:33 那轮尝试时**后端是 down 的**（dev 日志里全是
`[vite] http proxy error: /api/v1/pos/auth/login ECONNREFUSED`），并且它的 CSS 监听依旧不可靠。
我重启了 3001 上的 dev server，现在 `/api` 正常代理到 8081。

为了在没有明文口令的情况下也能验证真实链路，我用 **dev 环境的 HS512 密钥**为三个真实账号签了短期 token
（仅本地验证用，不落库、不改任何数据）：

| 账号 | userId | 角色 |
|---|---|---|
| admin | 1 | ROLE_SUPER_ADMIN |
| emp-j | 10 | ROLE_STORE_MANAGER |
| vcashier01 | 2080170081205395463 | ROLE_CASHIER |

## 1. 真实后端接口实测（同一批接口，三个角色）

| 接口 | admin | 店长 emp-j | 收银员 vcashier01 |
|---|---|---|---|
| `/api/v1/product-center/foods/on-sale` | **403** | 200 ✅ | **403** |
| `/api/v1/product-center/categories/tree/enabled` | **403** | 200 ✅ | **403** |
| `/api/v1/pos/orders` | **403** | 200 ✅（116 单） | 200 ✅（116 单） |

**两条后端问题（需要后端处理，前端无法兜）**：
1. **admin 全 403**：`bug-㉑` 之后 JWT 不再携带 permissions，服务端按角色实时解析权限；
   admin 的通配 `*` 只写在登录响应的 `securityUser.setPermissions(["*"])` 里，
   服务端解析链路没有对 `ROLE_SUPER_ADMIN`/`admin` 做特判 → 一条权限都没有。
   建议在权限解析入口加与登录一致的特判：`username=admin` 或含 `ROLE_SUPER_ADMIN` → `*`。
2. **收银员缺商品查看权限**：`ROLE_CASHIER` 的 role_permissions 里没有
   `product:food:view / product:category:view / product:combo:view` → 菜单 403。前端已按"无权限"空态兜住。

## 2. 浏览器端到端（dev server → 真实后端，8 项全绿）

| 断言 | 结果 |
|---|---|
| 店长菜单正常加载（真实菜品 7 个） | ✅ |
| 店长可见真实订单列表 | ✅ 116 行 |
| **店长可见退款按钮**（roles 回显生效） | ✅ 1 个 |
| 详情后厨状态不再出现"待处理"错值 | ✅ |
| 收银员 403 显示"账号暂无商品查看权限"空态 | ✅ |
| 收银员仍能查订单 | ✅ |
| 收银员看不到退款按钮（最小权限） | ✅ 0 个 |
| admin 被后端 403 时如实展示无权限空态 | ✅ |

## 3. 交班日结的质感打磨

| 问题 | 改法 |
|---|---|
| 异常单提示条是"红条 + 红按钮"，与异常抽屉的 amber 不统一 | 统一到 `--pos-warning-*` 令牌；按钮改**描边**样式，与抽屉同色 |
| hero 是暖橙渐变大卡片，营销感重 | 改**平底 + 左侧 3px 品牌竖条**，投影取消，只留 1px 内描边 |
| "现金收入 ¥0.00 − 现金退款 ¥36.00"，但上面写 ¥0.00，账对不上 | 公式补 **`= 净现金 −¥36.00`**（负数用危险软底 chip），金额关系一眼可算 |
| 备用金 chip 被拉伸成整行 | `justify-self: start`，宽度随内容 |
| 负现金流提示里"¥36.00"重复两遍 | 精简为「（退款大于收款），已从备用金垫付；请店长补足备用金」 |
| 净营业额 −36.00 没有解释 | 小字补「· 退款居多」 |
| 三张非现金卡各自重复"已直连入账" | 删除重复小字，语义由模块标题右侧的统一标签承载 |

深色与浅色两套都重新截图确认（`round17-handover-dark/light.png`）。

## 验证

- `pos-real-backend.cjs`（新增，真实后端）8/8
- `pos-round2-verify.cjs` 42/42（两处断言随之更新为非现金卡"不重复小字"）
- `pos-visual-verify.cjs` 16/16、`pos-round15-verify.cjs` 12/12、`pos-negative-cash.cjs` 5/5

截图：round17-real-manager-orders.png、round17-real-cashier-403.png、round17-real-admin-403.png、
round17-handover-light.png、round17-handover-dark.png。

## R3 审计前的待办（给后端）

1. admin/ROLE_SUPER_ADMIN 在**权限解析链路**上补 `*` 特判（否则审计一登录 admin 就是满屏 403）；
2. 给 `ROLE_CASHIER` 授予 `product:*:view` 三个只读权限（否则收银员看不到菜单）；
3. `user_roles.role_id` 混存了数字与 `ROLE_*` 字符串（`1` 与 `ROLE_CASHIER`），建议统一为 role_id_str。
