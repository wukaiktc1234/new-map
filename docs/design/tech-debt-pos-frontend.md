# 技术债：POS 前端（2026-10-29）

> 记录人：pos-ux ｜ 裁定人：Lead
> 规则：每条必须写「为什么现在不改」+「最小改法」+「风险」，避免变成无人认领的清单。

## TD-POS-001　KDS 操作失败时「无错误对象可读」，用户只看得到「操作失败」

**位置**
```
frontend-pos/src/components/kitchen/KitchenOrderCard.vue:213 / 222 / 237 / 262
（handleStartMake / handleComplete / handleServe 的 `if (!success)` 分支）
frontend-pos/src/stores/kitchen.ts（receiveOrder / startMake / completeMake / serveOrder 返回 boolean）
```

**现状**
```
store 的方法内部 catch 掉异常并 return false → 组件拿不到 error 对象
→ 只能 ElMessage.error('操作失败')，后端写的原因（如「菜品已售罄：X」）完全丢失
```

**为什么本轮不改（Lead 2026-10-29 裁定）**
1. 它不是「吞错误」，而是**根本没有错误对象可读**；
2. 要把 store 改成 `throw` 会影响 KDS 全部调用方，联调期做契约变更风险高。

**最小改法（零契约破坏，下一轮）**
```ts
// stores/kitchen.ts —— 不改返回类型、不 throw，只额外暴露一个 ref
const lastError = ref<string>('')
async function startMake(id: string): Promise<boolean> {
  try { /* … */ return true }
  catch (e) { lastError.value = resolveErrorMessage(e, ''); return false }
}
// 导出 lastError

// 组件侧
if (!success) ElMessage.error('操作失败' + (store.lastError ? '：' + store.lastError : ''))
```

**风险**：调用方无需改动（新增字段是附加的）；需注意并发多个卡片时 lastError 是「最后一次」，展示前应即时读取。

**验收**：人为让 startMake 后端返回 4xx（带 message）→ 卡片 toast 必须出现该 message。

---

## 已确认「保持现状」的两类（不属于本债务）

| 站点 | 裁定 | 理由 |
|------|------|------|
| `订单创建失败：未获取到订单ID`（usePayment.ts:111 / Payment.vue:665） | 保持 | 后端 200 但 data 缺 orderId 属**契约异常**，必须当场报出来；线上出现即 **P1 缺陷信号**（后端某分支漏了 orderId） |
