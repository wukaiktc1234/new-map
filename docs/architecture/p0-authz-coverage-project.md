# P0-系统性立项：授权覆盖率（authenticated ≠ authorized）

> **一句话**：全应用 **85.8% 的端点既无方法级也无类级 `@PreAuthorize`**；
> `SecurityConfig` 只有 `.anyRequest().authenticated()` —— **默认允许，不是默认拒绝**。
> 这是今晚五处跨人资金信息泄露能成立的根因。

## 1. 现状数字（实测）

```
total=2108 端点 ｜ 有门禁=299（14.2%）｜ 基线内缺口=1809 ｜ 新增缺口=0
来源：scripts/qa/preauthorize_coverage_gate.py（已含类级 @PreAuthorize 计算）
基线：scripts/qa/preauthorize_baseline.json（1809 条，只许降不许增）
```

## 2. 根因模型：两层缺口必须分开治理

```
SecurityConfig:  .anyRequest().authenticated()   ← 认证，不是授权
permitAll: /v1/auth/** /v1/public/** /v1/wecom/** /v1/users/check-* /v1/test/** /v1/ocr/** /v1/epson-printer/**

第一层（授权）   ：这个端点该不该允许这个角色调用      → @PreAuthorize / 权限码
第二层（数据边界）：调用后能看到「谁的数据」          → employee / terminal / store 归属校验
→ 今晚修的全是第二层；第一层还差 1809 个端点
→ 而「有第一层、无第二层」同样造成泄露：vtest01 有 pos:shift:manage → 门禁放行 → 若内部不按 employee 过滤即泄露
```

## 3. 域清单与优先级（按「泄露后果严重性」，不只看域）

| 优先级 | 域 | 代表端点 | 当前门禁 | 权限码现状 | 缺口 | 涉资金/钱箱 | **泄露后果（排序依据）** |
|---|---|---|---|---|---|---|---|
| **P0-1** | 班次 / 钱箱 | `/v1/pos/auth/current-shift`、`/shift/opening-cash`、`/shift/stats`、`/shift/exceptions`、`/auth/shift/today` | 部分有 `pos:shift:manage`；`auth/*` 无 | 有 | **5（今晚已修数据边界）** | **是** | **交班面板用他人 openingCash 算「应上交现金」→ 直接对不上账**（实测 B 曾拿到 A 的 321.00） |
| **P0-2** | 财务 / 资金 | 财务记录、资金流水、凭证、预算类 | 多为无 | 部分有 | 待精确统计 | **是** | 可读他人/全店资金流水与余额 → 与 P0-1 同级甚至更高 |
| **P1-1** | 订单 / 支付 | `/v1/pos/orders`、`/orders/order/{orderNumber}`、`/byOpenid`、`/byOrderNumber`、`/orders/order/{id}/receipt` | 类级（角色）有 | `pos:order:*` 齐全 | **6（P2-15）** | 是（金额） | 跨终端查他人订单金额/支付方式；**可重打他人小票** |
| **P1-2** | 会员 / 券 | `MemberController`（13 端点有 `member:*`）、`PosCouponController` | 有 | `member:*` 齐全 | 低 | 是（余额/券） | 会员手机号/余额跨店可见（若无 store 边界） |
| **P2-1** | 基础数据 | `PosApiController` 菜单、`DiningTableController`、`CallNumberController`、`YoloProductRecognitionController` | 菜单/桌台类级有；**CallNumber 无** | `call:*` **= 0 条（未设计）** | CallNumber 11 端点 / 7 写 | 否 | 叫号/桌台被任意登录用户操作（非资金） |
| **P2-2** | 其余业务域 | HR / 采购 / 库存 / 报表等约 200 个 controller | 多为无 | 视域而定 | **主体 1809** | 部分是 | 取决于域（HR=隐私、采购=价格） |

## 4. 每域产出物标准（缺一不算完成）

```
① 「角色 × 动作 × 权限码」矩阵（谁 → 能做什么 → 需要什么码）
② 改动：权限码登记（若码表缺）+ 端点注解 + 数据边界校验
③ A/B 实测：B 角色访问该域【他人数据】→ 必须 403 或 data:null，贴两次响应原文
④ 基线下降：该域缺口从基线扣减并重新生成 baseline（只许降）
```

## 5. 纪律三条

```
① 不追旧账、只防新增（棘轮已接入 preflight：基线 1809，新增即 BLOCK）
② 每域做完必须【降低基线数字】，否则视为未完成
③ 没有权限码的域，先产出「权限模型提案」，不写代码（CallNumber 已按此处理：call:* = 0 条 → 不硬造）
```

## 6. 验收判据

```
· 基线从 1809 逐域递减（每次 --write-baseline 后贴新旧数字）
· 每域必须有「B 角色访问该域他人数据 → 403 / data:null」实测
· 门禁长期保持 新增缺=0
```

## 7. 样板域（今晚已完成，作为标准示范）

```
域：班次/钱箱（P0-1，最高优先级）
修复 5 处数据边界：current-shift / opening-cash / stats / exceptions / shift/today
实测：B(vtest01) 全部 data:null 或 0 条；A(vcashier01) 正常返回本人数据
关键认知：@PreAuthorize('pos:shift:manage') 对 vtest01 是【放行】的 → 权限门禁挡不住这类泄露
```