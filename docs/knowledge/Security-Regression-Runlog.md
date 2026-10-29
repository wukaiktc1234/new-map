# 安全回归运行日志（S1~S6）

> 本文档记录**实际运行证据**，不是测试计划。每次跑完把关键数值追加到下面。
> 脚本：`scripts/qa/security_regression.py`（`preflight --full` 内置为硬门禁）

---

## 1. 绿的那两次（2026-10-29，服务已修复）

### 运行 A（tag=SEC1029132215）

```
[SELF-CHECK] 关联键样例：ORD202610272350320001 / unit_price=1800
[SELF-CHECK] 有明细订单=131；菜品(可售/售罄)=6/2；问题=0

S1 [PASS] price=0.01 不被采信 -> code=0 order=W20261029015 DB total=80 期望 80 分（本单 0.01 元=1 分，若采信则为 1）
S1 [PASS] price_override 打点 -> 命中日志 2 个
S1 [PASS] 对照：合法价 -> order=W20261029016 DB total=80
S2 [PASS] 售罄菜 FD260926002 -> code=400 菜品已售罄；orders 135 → 135
S2 [PASS] 对照：可售菜 -> code=0；orders 135 → 136
S3 [PASS] employeeId=admin -> code=403 不得为他人开班；pos_shifts 74 → 74
S3 [PASS] 对照：自己 employeeId -> code=0
S4 [PASS] 两 active 班次 -> mine(566) closed=1；other(567) still_open=1
S4 [PASS] 对照：单班次 -> completed
S5 [PASS] 权限 34 个，含 refund:view、无写权限；vcashier01 退款 code=403；店长 code=0
S6 [PASS] 响应 orderItems[0].price=0.8 元 → 80 分 == DB 80 分
S6 [PASS] 对照：合法价同样一致
[INFO] 夹具清理 -> orders_left=0；shifts_left=0
总计：PASS=14 FAIL=0
EXIT=0
```

### 运行 B（tag=SEC1029132305，复跑验证稳定性）

```
结果与运行 A 完全一致：PASS=14 FAIL=0 / EXIT=0 / orders_left=0；shifts_left=0
S4：mine(574) closed=1；other(575) still_open=1
S6：响应 0.8 元 → 80 分 == DB 80 分
```

---

## 2. 两条「由红转绿」的修复验证（证明修复有效，不是新写的测试）

| 用例 | 修复前（红） | 修复后（绿） | 证据 |
|---|---|---|---|
| **S4 跨人交班** | N4 曾 FAIL：交班 API 只带 terminalId → 产品按「该 terminal 最近一个 active 班次」关闭 → **关掉别人的班次**，自己的仍 open（连跑 3 次一致） | `mine closed=1；other still_open=1` | 同一脚本、同一夹具做法（同 terminal 两 active 班次），仅产品行为变化 |
| **S6 响应价回显** | 曾 FAIL（发现 D）：`PosOrderCreateServiceImpl:772 info.setPrice(item.getUnitPrice())` 用**请求价**回填响应 → 篡改价下单时响应 `orderItems[0].price=0.01`，而 DB 存 25.00 元 | 响应 `0.8 元 → 80 分 == DB 80 分` | 断言未改（`响应换算成分 == DB 分`），只有产品行为变 |

---

## 3. 自检（[NOT_READY] EXIT=2）的两次实际触发 —— 证明自检不是摆设

两次都发生在**后端正在重启**（fund-500 改完代码重启中）时：

```
$ python scripts/qa/security_regression.py
[SELF-CHECK] 关联键样例：ORD202610272350320001 / unit_price=1800
[SELF-CHECK] 有明细订单=131；菜品(可售/售罄)=6/2；问题=0
[ERROR] 后端连不上（HTTP -1）→ EXIT=2
EXIT=2
```

关键点：**没有产出假 FAIL**。
如果脚本在后端没起时直接跑断言，会得到 6 组全红（连接失败），然后被人当成「安全防护失效」——
那才是真正有害的噪音。这就是头部 `[SELF-CHECK]` + 就绪检查存在的理由。

---

## 4. 待做的攻击面（Lead 2026-10-29 登记，本轮不做）

| # | 攻击面 | 为什么重要 | 初步思路 |
|---|---|---|---|
| A-1 | **IDOR**（越权访问他人订单） | 改 orderId/orderNumber 就能看别人订单，是最常见越权 | 用 A 账号登录，遍历 B 账号的 orderNumber → 期望 403/404；对照：自己的订单 → 200 |
| A-2 | **批量导出限流** | 导出接口无频控 → 一次拖走全量数据 | 连续 N 次导出 → 期望触发限流（429/业务码）；对照：单次导出 → 200 |
| A-3 | **并发竞态（同 key 并发下单）** | 同一 idempotencyKey 并发 → 可能重复建单/重复扣款 | 同一 key 并发 5 请求 → 期望只 1 单；对照：不同 key → 5 单 |
| A-4 | **CSRF** | 无 token 的跨站表单提交能否生效 | 用无 Origin/Referer + 旧式表单 Content-Type 发写请求 → 期望被拒；对照：带正确头的 → 200 |

**原则**：新用例一律遵守「**绕过 UI，直击 API**」—— 只按前端能点的路径测，测不出真实攻击面（S2 售罄菜就是活例子）。

---

## 5. 怎么跑 / 怎么挂门禁

```bash
python scripts/qa/security_regression.py              # 全跑（约 20-40 秒）
python scripts/qa/security_regression.py --only S4    # 单组
python scripts/qa/security_regression.py --keep       # 保留夹具（排查用）
python scripts/qa/preflight.py --full                 # 内置：安全回归失败 → BLOCKED exit 2
```

EXIT：`0` 全 PASS / `1` 有 FAIL / `2` 未就绪（自检未过或后端没起，**不做断言**）。