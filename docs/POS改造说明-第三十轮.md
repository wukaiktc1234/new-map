# 第三十轮：登录/登出状态残留 + 开班状态联动

> 两个串号/状态不同步缺陷修复，新增 14 项 E2E；20 套件 **322 项全绿**。

## 缺陷 1：换账号后购物车 / 挂单残留

**根因**：
- 挂单存在 `localStorage['heldOrders']`，早期注释明确写着「挂单是收银员显式保存的单据，登出保留」→ 换人后仍能看到上一位（甚至几天前）的挂单；
- 购物车 `pos-cart` 虽然在清理列表里，但清理仅发生在登出接口 finally 分支，且没有 Pinia 层 `$reset`，`hanging-order` store 的内存数组一直是旧的。

**修复**（新增 `utils/resetAppState.ts`，两档清理，别混用）：

| 入口 | 清理范围 |
|---|---|
| `resetStoresInMemory()` | 只 `$reset` 内存 store（同账号重新登录用） |
| `resetBusinessState()` | 业务缓存（购物车/挂单/客显/草稿/班次标记）+ 内存 store，**不动凭证** |
| `clearAuthSession()` | 以上 + `pos-token` / `pos-user` / `pos-terminal`（退出登录用） |

挂点：
- `Login.vue` 登录成功：**发现换了人**（`pos-session:user` 比对）→ `resetBusinessState()`；同一账号重登只 `resetStoresInMemory()`（不误删本人挂单）；
- `PosTopBar` 退出登录 / 改密后重登：`clearAuthSession()`；
- 路由守卫进首页前 `ensureSessionUser()` 兜底（覆盖「直接改 localStorage 再刷新」这类非正常切换）。

`posCache.ts` 的 sessionStorage 清理按前缀扫 `pos-draft:` / `pos-session:`，草稿不会跨账号残留。

## 缺陷 2：开班成功后警告条不消失

**根因**：`handleShiftStarted()` 只做了两件事 —— toast + 关弹窗。既没写本地班次标记，也没更新任何状态，警告条的 `v-if` 仍读 `shiftState='no-shift'`，所以必须手动刷新（刷新时又因为没标记，重新问后端才纠正）。

**修复**：
1. store 新增 `currentShift` + `hasActiveShift` + `setCurrentShift()`（单一事实源）；
2. 开班成功 → 清 `shiftState` + `markShiftStarted()` + `setCurrentShift()` + `refreshStats()`；
3. 警告条 `v-if="shiftStateText && !showShiftHandover && !shiftStore.hasActiveShift"`；
4. 交班按钮新增 `data-shift-active` 与 `.is-active` 高亮，`Home` 传 `:shift-active="shiftStore.hasActiveShift"`。

## E2E（`pos-round30-verify.cjs`，14 项）

用例 1：A 登录 → 加购 → 挂单 → 退出 → B 登录
| 断言 | 结果 |
|---|---|
| A1 A 加购后购物车 1 件 | ✅ |
| A2 A 挂单成功 heldOrders≥1 | ✅ |
| A3 退出后回到登录页 | ✅ |
| A4 退出后 `pos-cart` 已清 | ✅ |
| A5 退出后 `heldOrders` 已清 | ✅ |
| A6 退出后 token 已清 | ✅ |
| A7 B 登录后购物车为空 | ✅ |
| A8 B 登录后取单列表为空 | ✅ |

用例 2：无班次 → 开班
| 断言 | 结果 |
|---|---|
| B1 无班次显示警告条 | ✅ |
| B2 无班次交班按钮不高亮 | ✅ |
| B3 开班后警告条**立即**消失 | ✅ |
| B4 交班按钮立即高亮（`is-active`） | ✅ |
| B5 开班写入本地班次标记 | ✅ |
| B6 刷新后仍在班、不弹开班 | ✅ |

## 回归

20 套件 **322 项全绿**。

期间 round8 因「登录清挂单」失败过一次 —— 它把挂单当夹具在登录前种进 localStorage。已按新语义改为同一账号重登不清挂单（换人才清），既修 bug 又不误删本人单据。

截图：round30-before-start.png / round30-after-start.png / round30-switch-account.png。
