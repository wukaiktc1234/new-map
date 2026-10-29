# 第二十一轮：异常单处理抽屉重构（可操作 + 账目后端闭环）

> 断头路（只有提示、没有操作）已打通：卡片上直接「作废 / 补收款」，操作后账目由服务端整体下发。

## 新增文件

| 文件 | 职责 |
|---|---|
| `src/api/shiftExceptions.ts` | 异常单 / 班次统计 API 契约（含 Idempotency-Key） |
| `src/stores/shiftHandover.ts` | Pinia store：shiftStats 整体替换、pendingExceptionCount、作废/补收/扫码 |
| `src/composables/useScanPaymentPolling.ts` | 扫码轮询（2s / 120s，paid 前不动账） |
| `src/components/pos/shift/ExceptionDrawer.vue` | 抽屉：待处理/已处理 Tab + 15s 轮询 + 409 处理 |
| `src/components/pos/shift/ExceptionCard.vue` | 卡片：单号/金额/开单员/类型徽标 + 作废/补收款 |
| `src/components/pos/shift/ResolveDialog.vue` | 补收款：现金（找零）/ 扫码（二维码 + 轮询） |
| `src/components/pos/shift/VoidDialog.vue` | 作废：枚举原因 + 其他必填备注 + 二次确认 |

## 关键约束的落地方式

1. **废除前端自减**：`voidOrder/resolveCash` 成功后只做 `refreshAll()`（列表 + stats），
   页面上的现金收入/钱箱应有/应上交/异常 badge 全部改读 `shiftStore.stats`；
   只有在后端 `shiftStats` 缺失时才退回明细口径（并保留降级路径，防止后端未发布时页面空白）。
2. **交班按钮**：`:disabled="endingShift || pendingExceptionCount > 0"`，文案变「先处理异常单」，
   外层 `el-tooltip` 说明「还有 N 笔异常单未处理」。
3. **15s 轮询**：抽屉打开时 `setInterval(store.refreshAll, 15000)`，关闭即清。
4. **409**：`handleConflict()` 统一识别 `status === 409` → Toast「该订单已被其他终端处理，已为你刷新列表」+ `refreshAll()`。
5. **防连点**：`submitting` 状态 + 按钮 loading；`idempotencyKey` 由 `buildIdempotencyKey(action, orderId)` 生成并缓存，重试复用。
6. **扫码不乐观更新**：`createScanPayment` 只建单；轮询 `paid` 后才 `store.onScanPaid()` 触发刷新。

## 验证（新增 27 项断言，全绿）

| 断言 | 结果 |
|---|---|
| 有待处理异常 → 交班按钮 disabled + 文案「先处理异常单」 | ✅ |
| tooltip 说明阻断原因 | ✅ |
| badge 用后端口径（后端 3，列表 2） | ✅ |
| 抽屉 Tab 待处理/已处理、卡片四要素、双按钮 | ✅ |
| 作废：未选原因禁用、选「其他」必填备注、二次确认、幂等键、body 正确 | ✅ |
| 作废后 badge 3→2（后端重下发） | ✅ |
| 补收款：找零 5.00、连点仅 1 次请求、金额正确、账目 60→90 | ✅ |
| 扫码：2s 轮询；paid 前 statsCalls 不变（无乐观更新）；paid 后刷新并关窗 | ✅ |
| 409 → Toast + 自动刷新 | ✅ |

回归：全 14 套件 253 项断言全绿。

截图：round21-exception-drawer.png、round21-exception-drawer-after.png。

## 对接后端需实现

```
GET  /v1/pos/shift/stats?terminalId             → ShiftStats
GET  /v1/pos/shift/exceptions?terminalId&tab    → ExceptionOrder[]
POST /v1/pos/shift/exceptions/{id}/void         (Idempotency-Key)
POST /v1/pos/shift/exceptions/{id}/resolve      (Idempotency-Key)
POST /v1/pos/shift/exceptions/{id}/scan         (Idempotency-Key) → { payNo, qrContent, expiresAt }
GET  /v1/pos/shift/scan/{payNo}                 → { status: pending|paid|closed|expired }
```

冲突返回 **409**；写接口必须按 Idempotency-Key 去重。
