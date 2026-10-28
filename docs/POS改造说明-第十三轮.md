# 第十三轮：断流与越权修复（KDS 路径 / 售罄防呆 / 退款权限 / 交班 terminalId）

> 4 项后端联动问题已修。验证：+12 项新断言，累计 **176 项全绿**（原有 164 项无回归）。

---

## 0. 先说"改了没生效"怎么确认

新增**构建标记**：右上角头像菜单里有一行 `前端版本 r12.2026-10-28`，控制台也会打印 `[POS] 前端版本 …`。
刷新后看不到这行 = 浏览器跑的还是旧包，不是代码没改。

三种常见原因：
1. 跑的是 `npm run dev` 之外的静态服务（例如后端 `static/` 里的旧 dist）→ 需要重新 `npm run build` 并把 `frontend-pos/dist` 覆盖过去；
2. `index.html` 被强缓存 → Ctrl+Shift+R 硬刷新，或服务端给 index.html 加 `Cache-Control: no-store`；
3. 打开了多个标签页，旧标签仍在跑旧 bundle → 关掉只留一个。

## 1. KDS 接口 404：路径全量修正

`/v1/kitchen-order/*` → **`/v1/kitchen/orders/*`**，共 12 处（全部在 `src/api/kitchen.ts`）：
active/full、recent/full、{id}、{id}/full、list、receive、start-make、complete-make、serve、cancel、priority、statistics/{storeId}。

静态断言（直接扫打包产物）：产物中已无 `/v1/kitchen-order`，且存在 `/v1/kitchen/orders`。

## 2. 售罄/下架防呆：三层拦截

| 层 | 位置 | 行为 |
|---|---|---|
| 视图 | `MenuSection.vue` | 卡片置灰 + 遮罩文案（已售罄 / 已下架）+ `aria-disabled`，点击不派发事件 |
| 组合 | `useCart.addToCart` | 再判一次，命中直接 Toast 并 return（键盘/别的入口也拦得住） |
| 数量 | `useCart` | 有明确库存时不允许超过库存（提示"库存仅剩 N 件"） |

判定兼容后端字段差异：`stock<=0`、`soldOut`、`available=false`、`onSale=false`、
`status/saleStatus` 命中 `0 / SOLD_OUT / OFF_SHELF / STOP / DISABLED …`。

链路补全：`foodVOToDish` 与 `Dish` 类型补透传 `status/saleStatus/soldOut/available/onSale`——
之前这些字段在 VO→Dish 转换时被丢弃，前端根本看不到"下架"状态（这也是之前置灰只对 stock=0 生效的原因）。

## 3. 退款权限：默认拒绝 + 三重放行 + 403 记忆

新增 `composables/usePermission.ts`：

```
放行条件（任一）：
  ① pos-user.permissions/authorities/perms 含 order:refund 或 "*"
  ② roles 属于店长/管理员类（admin / manager / store_manager / owner / 店长 …）
  ③ 运行时会话内被授予过（pos-perm-granted）
一票否决：
  ④ 后端返回过 403（记入 pos-perm-denied）→ 本会话永久隐藏入口，直到重新登录
默认：以上都不满足 → 无权限（宁可少显示按钮，也不让收银员点出 403）
```

落地位置：
- `OrderQueryDialog`、`OrderDetailDialog`：无权限时退款按钮**不渲染**；
- `Home.refundOrder`：调用前先判权限（不发请求），捕获 403 时给出明确提示并写入 denied；
- `api/request.ts`：错误对象补 `status`，让上层能识别 403。

## 4. 交班 terminalId：硬校验

`ShiftHandover.vue` 新增 `ensureTerminalId()`：terminalId 为空或是 `pos-default` 兜底值时，
**直接拦住并提示"请退出后重新登录本机"**，不再带着错误终端号去查统计/结束班次；
`shift/end` 提交时对 terminalId 做 `trim()`，与登录写盘值保持完全一致。

---

## 验证（新增 12 项）

```
K1 打包产物无 /v1/kitchen-order           ✅
K2 打包产物含 /v1/kitchen/orders          ✅
P1 售罄卡片置灰                           ✅ out-of-stock
P2 售罄遮罩文案「已售罄」                  ✅
P3 status=SOLD_OUT 也置灰标「已下架」      ✅
P4 点击售罄/下架不加购                     ✅ cartItems=0
P5 stock=5 连点 6 次封顶 5 件并提示        ✅
R1 无权限时退款按钮不渲染                  ✅ 0 个
R2 店长角色可见退款按钮                    ✅ 1 个
R3 后端 403 明确提示                       ✅
R4 403 后本会话不再显示退款入口            ✅ pos-perm-denied=["order:refund"]
B1 账号菜单显示构建标记                    ✅ 前端版本 r12.2026-10-28
```

回归：round2 42 / retrieve 30 / round8 15 / round12 25 / order-query 14 / visual 16 / hold 16 / negative 5 / payment 1 —— 全绿。

截图：round13-soldout-guard.png、round13-refund-hidden.png。
