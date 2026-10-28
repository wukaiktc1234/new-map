# 第十四轮：你说的"改了没生效"——查到的四个真实 Bug

> 结论：**不是刷新问题，也不是我虚报**。你的 Vite dev server 一直在吐**旧的 CSS 模块**，
> 加上三个真 Bug（外壳硬编码浅灰、售罄遮罩深色下不可见、顶栏实际需要 1800px）。
> 验证：dev server 实测 8 项 + 打包产物 176 项断言全绿。

---

## 0. 最关键的证据：dev server 缓存是旧的

直接对比"dev server 吐出来的 CSS"与"磁盘上的文件"：

| | 长度 | 是否包含深色画布变量 |
|---|---|---|
| 重启前的 dev server | 5580 字节 | ❌ 没有 `--pos-bg-canvas: #181819` |
| 磁盘上的 `styles/pos/variables.css` | 有该行（第 112 行） | ✅ |
| 重启后的 dev server | 5609 字节 | ✅ 有 |

```bash
# 复现命令（你可以自己验）
curl -s http://localhost:3001/src/styles/pos/variables.css | Select-String 'pos-bg-canvas'
```

所以浏览器拿到的是"没有深色画布色"的旧 CSS——**不管刷新多少次都不会变**，因为 Vite 的文件监听在这台机器/这个盘符上没有跟上（JS 变了、CSS 没变，正好解释"控制台版本号是新的、样式却没变"）。

我已经把 3001 上的旧进程（PID 2548）停掉并重新起了 `npm run dev`，现在它吐的是最新 CSS。

## 1. 真 Bug：应用外壳硬编码浅灰（"白边"根因）

`App.vue` 里：
```css
.pos-app { height: 100%; background: #f5f5f5; }   /* 写死的浅灰，深色模式下就是一圈白边 */
```
改成跟随主题：
```css
.pos-app { background: var(--pos-bg-canvas, var(--pos-bg-primary)); }
[data-theme='dark'] .pos-app { background: #181819; }
```

## 2. 真 Bug：`.main-content` 在深色下仍是浅灰

`--pos-bg-canvas` 在深色块里没生效（旧 CSS 里根本没这个变量），`.main-content` 用了它的**兜底浅灰**
`var(--pos-bg-canvas, #f2f3f5)` → 底部/边缘一条浅色。现在补了显式深色规则：

```css
[data-theme='dark'] .main-content { background: var(--pos-bg-canvas, #181819) !important; }
```

dev 实测（1490×703）：四角像素 tl/tr `rgba(30,30,32,.88)`、bl/br `rgb(24,24,25)` —— 全部深色，没有白边。

## 3. 真 Bug：售罄遮罩在深色下"整卡空白"

遮罩文字用了 `--pos-text-inverse`，深色主题里它等于 `#1e1e1f`（深字）→ 深字叠深底，
所以置灰的卡片看着像"空盒子"。改成固定白色胶囊文案 + 半透明黑底：

```css
.sold-out-overlay { background: rgba(12,12,14,.55); backdrop-filter: blur(2px); }
.sold-out-overlay span { color:#fff; background: rgba(0,0,0,.45); border-radius:9999px; padding:3px 10px; }
```

## 4. 真 Bug：顶栏在 1490 宽时会裁掉最后一个按钮

实测（1800 宽、带文字）：`store-info 369px + right-section 1367px + 间距/内边距` = **需要约 1800px**。
所以 1490 的窗口里最后一个"主题切换"被裁成半个图标——正是你截图右上角那个残缺图标。

修法：断点提到 **1780**（低于它只留图标），并给每个按钮补 `title` 便于悬停识别。
现在 1490 实测：9 个按钮全部完整显示、无裁切。

## 5. 上一轮遗留的两个逻辑 Bug（这次一并修掉）

| Bug | 说明 | 修法 |
|---|---|---|
| 权限判定不响应式 | `allowRefund` 是 setup 时算一次的常量；403 之后按钮要**重开弹窗**才消失 | 改成共享响应式快照（`snapshot` ref），`markDenied` 立刻触发重渲染 → 403 后按钮**当场消失**（新增断言 R5） |
| 购物车「+」不受库存约束 | 加购拦住了，但购物车里的 + 号能把数量加过库存 | `CartItem` 记录 `stock`，`increaseItem` 超限即提示并拒绝 |

## 6. 后端配合（已改，纯新增字段）

`PosAuthServiceImpl.login` 现在把已经算好的授权结果回显到 `userInfo`：
```java
userInfo.put("roles", securityUser.getRoles());
userInfo.put("permissions", securityUser.getPermissions());
```
只回显、不新增判断、不放宽任何限制。前端据此决定退款入口：
- `ROLE_CASHIER`（收银员）→ 看不到退款
- `ROLE_STORE_MANAGER` / `admin` / 含 `order:refund` → 看得到
- 后端仍会 403 兜底，前端收到后立即隐藏入口

---

## 验证

**dev server（localhost:3001，Vite 5.4.21）实测 8 项全绿**：
```
D1 首页渲染成功                    ✅
D2 控制台版本号 r12.2026-10-28      ✅
D3 视口四角深色（无白边）           ✅ rgba(30,30,32,.88) / rgb(24,24,25)
D4 根容器铺满 1490×703              ✅
D5 售罄卡片置灰                     ✅
D6 点击售罄不入车                   ✅
D7 收银员无退款入口                 ✅
D8 店长有退款入口                   ✅
```

**打包产物回归**：round2 42 / retrieve 30 / round8 15 / round12 25 / round13 15 / order-query 14 / visual 16 / hold 16 / negative 5 / payment 1 —— 全绿。

截图：round14-dev-1490-dark.png（1490 深色，无白边、顶栏完整、售罄文案可见）。
