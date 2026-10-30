# 第二十六轮：权限契约锁定 + 明细一致性 + 抬头容错

> 三条都做成了**可断言**的契约，16 套件 281 项全绿。

## 1. 403 契约锁死

**拦截器层**（`api/request.ts`）：HTTP 200 却用 `body.code=401/403` 表达鉴权失败 = 反模式，现在：
- 直接 `Promise.reject({ status: 403, fromBodyCode: true })`；
- `console.error` 明确指出：接口 URL + “后端必须改为真实 HTTP 403，否则权限判定与刷新链路都会失效”。

**登录页**（`Login.vue`）：
- 预检 `GET /v1/pos/shift/stats`；真实 403 或 body.code=403 都按“无权限”处理；
- 命中反模式时额外 `ElMessage.warning('后端以 200+code=403 返回权限错误…')`；
- 失败时执行 `purgeSession()`：清 `pos-token` / `pos-user` / `pos-terminal` + `clearCapabilities()` + Pinia `store.reset()`，杜绝“半个登录”。

断言：C1 真实 403 拦下登录、C2 三个存储键全空、C3 200+code=403 同样拦下、C4 控制台有契约违约提示。

## 2. 明细入口数据一致性

- 抽屉表头改为与统计表同源：`共 <paidOrders.length> 笔 · 合计 <后端 netRevenue>`；
- 带筛选时显示「已筛选：现金 · 查看全部」，一键清筛选；
- 行内「详情」净额 = 该行小计（退款按负数计入）。

断言：C5 全量笔数一致（drawer=2 / table=2）、C6 全量金额一致（90.00 / 90.00）、C7 按方式净额一致（60.00 / 60.00）。

## 3. 抬头容错

| 字段 | 有数据 | 缺数据占位 |
|---|---|---|
| 收银员 | 班次 employeeName | 登录写入的 pos-user → **未知收银员** |
| 班次 | `班次进行中 #7` | **暂无班次** |
| 时间 | `开始于 09:00` | **今日汇总** |

断言：C8/C9/C10 占位符正确；C11 抬头渲染无异常（pageerror = 0）。

## 顺手修掉一个测试环境的假崩溃

C11 一开始总报 `Uncaught SyntaxError: Unexpected token ':'`。CDP 追到源头是 **SockJS 的 JSONP 兜底**：
测试环境没有真实 WS 端点，SockJS 插入 `<script src="/api/ws/.../jsonp?...">`，被 API mock 成 JSON 后浏览器直接语法错误。
已在夹具里 `page.route('**/api/ws/**', abort)` 屏蔽 —— 这是夹具问题，不是应用缺陷（生产有真实 WS 端点）。

## 全量

16 套件 **281 项全绿**（round2 43 / round21 27 / retrieve 30 / round12 25 / round18 16 / visual 16 / hold 16 / round13 15 / round8 15 / round24 14 / order-query 14 / round20 13 / round15 12 / round26 11 / round16 9 / negative 5）。

截图：round26-detail-consistency.png。
