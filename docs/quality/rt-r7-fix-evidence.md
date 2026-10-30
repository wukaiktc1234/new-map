# R7 红队整改实录 — 攻击前 vs 攻击后（RT-1 ~ RT-6）

> 产物：AUDIT-R7-FIX-20261030
> 靶环境：本机 8081 主实例（DB `food_traceability`）、8083 隔离实例（DB `ft_gate_rt7`，pg_dump 全量复制）
> 纪律：不留「HTTP 200 + body 500」；不留 try-catch 吞异常；不信任任何未经白名单校验的请求头

---

## 0. 结论速览

| 编号 | 缺陷 | 修前实测 | 修后实测 | 证据来源 |
|---|---|---|---|---|
| RT-1 P0 | 限流键源可伪造 | 轮换 `X-Real-IP` **6/6 全 200** | 第 6 次 **429**，且换成全新伪造 IP 仍是 429 | §2.1 |
| RT-2 P1 | 真 500 未落地 | `POST /v1/appeals/NOPE/process` → **HTTP 200** + body code 500 | **HTTP 500** + body 500 + `X-Trace-Id` + 日志同 ID | §2.2 |
| RT-3 P1 | 二次作废 → 唯一索引 500 | 二次 void → **HTTP 500**，body 里带裸 SQL（`uk_exception_ops_order`） | **HTTP 409**「订单已是【已取消】状态，无需重复作废」，零写入 | §2.3 |
| RT-4 P1 | 厨房 try-warn 吞异常 + 库存不回补 | 订单 cancelled 而 kitchen_order 仍 pending；`foods.stock` 不回补（只打 metric） | 同事务：KO `pending→cancelled`；stock `1051→1053`、`893→894`，且只回补一次 | §2.3 |
| RT-5 P2 | 内存 Map 无界 | — | 窗口清扫 + 50k 硬上限淘汰（单测覆盖） | §3 |
| RT-6 P2 | `securityEnabled=false` 一键全裸 | `anyRequest().permitAll()` 分支存在 | 分支删除，安全链恒开启，启动日志 `mode=enforced` | §1.5 |

单元测试：`RateLimitFilterTest / GlobalExceptionHandlerTest / PosExceptionTxBeanTest / TtlOrderCloseTxBeanTest / ExceptionOpsMapperSqlContractTest` **19 tests, 0 failures**。

---

## 1. 改了什么（文件级）

| # | 文件 | 变更 |
|---|---|---|
| RT-1 | `security/filter/RateLimitFilter.java` | 新增 `resolveClientIp()`：默认只信 `getRemoteAddr()`；新增 `rate-limit.trusted-proxies`（IP/CIDR）白名单，命中才解析 XFF/X-Real-IP，XFF 从右往左取第一个非代理地址；新增窗口清扫 `sweepExpired()` + `MAX_COUNTER_KEYS=50000` 硬上限淘汰；启动打印 `[SECURITY] rate_limit keySource=...` |
| RT-1 | `resources/application.yml` | 新增 `rate-limit.trusted-proxies`（默认空 = 一个代理都不信，支持 `RATE_LIMIT_TRUSTED_PROXIES` 覆盖） |
| RT-2 | `common/exception/GlobalExceptionHandler.java` | NPE / RuntimeException / Exception 三个兜底统一走 `internalServerError()`：`ResponseEntity.status(500)` + `X-Trace-Id` + MDC 日志；顺带把 `HttpMessageNotReadableException` 从 500 纠正为真 400 |
| RT-3 | `mapper/ExceptionOpsMapper.java` | `casCancel` 增加 `AND COALESCE(order_status,0)=0` |
| RT-3 | `service/impl/PosExceptionTxBean.java` | CAS 0 行 → 查当前状态 → 409 文案分流；新增 `insertOpsOrThrow409()` 捕获 `DataIntegrityViolationException` → 业务 409 |
| RT-3 | `controller/pos/PosExceptionController.java` | BusinessException 按 `httpStatus` 优先映射（原只认 40903，409 会被降级成 400）；`respond()` 状态码通用映射 |
| RT-4 | `service/impl/PosExceptionTxBean.java` | 厨房取消失败**直接抛**（不再 try-warn）；条件更新幂等；作废后调用库存回补 |
| RT-4 | `service/impl/PosOrderStockRestorer.java`（新） | 把 TTL 关单的 `order_items→foods.food_code→addStock` 抽成唯一实现，TTL 与异常单作废共用；`propagation=MANDATORY` 强制同事务 |
| RT-4 | `service/impl/TtlOrderCloseTxBean.java` | 改为委托 `PosOrderStockRestorer`（口径只剩一份） |
| RT-5/6 | `security/config/SecurityConfig.java`、`security/filter/JwtAuthenticationFilter.java` | 删除 `permitAll()` 后门与 JWT 跳过分支；开关置 false 只在启动时 ERROR 报警 |

---

## 2. 攻击前 vs 攻击后（真实命令与真实输出）

### 2.1 RT-1 限流键源

**攻击前**（旧构建，8082，`POST /api/v1/auth/login`，阈值 5/60s）：

```text
[CONTROL] 固定源 IP，6 次：200 200 200 200 200 429      ← 阈值本来是对的
[ATTACK]  每次换 X-Real-IP=10.7.i.1，6 次：200 200 200 200 200 200   ← 绕过成立
```

**攻击后**（新构建，8083，`POST /api/v1/pos/auth/login`）：

```bash
for i in 1 2 3 4 5 6; do
  curl -s -o /dev/null -w '%{http_code}\n' -X POST http://127.0.0.1:8083/api/v1/pos/auth/login \
       -H "X-Real-IP: 10.7.$i.1" -H 'Content-Type: application/json' \
       --data-binary @.verify-tmp/login-bad.json
done
```

```text
atk[1] X-Real-IP=10.7.1.1 => HTTP 200
atk[2] X-Real-IP=10.7.2.1 => HTTP 200
atk[3] X-Real-IP=10.7.3.1 => HTTP 200
atk[4] X-Real-IP=10.7.4.1 => HTTP 200
atk[5] X-Real-IP=10.7.5.1 => HTTP 200
atk[6] X-Real-IP=10.7.6.1 => HTTP 429   {"code":429,...,"retryAfter":"60"}
[probe] 7th（全新伪造 X-Real-IP=203.0.113.77）=> HTTP 429
[probe] 8th（只带 X-Forwarded-For=198.51.100.42）=> HTTP 429
```

**可信代理链**（`RATE_LIMIT_TRUSTED_PROXIES=127.0.0.1/32`，Nginx 场景）：

```text
XFF: 198.51.100.7, 127.0.0.1   ×6  → 200 200 200 200 200 429    ← 按真实客户端分桶
XFF: 198.51.100.8, 127.0.0.1   ×1  → 200                        ← 不同客户端不连坐
XFF: 10.9.9.1, 198.51.100.7, 127.0.0.1 → 429                    ← 最左段伪造无效
```

启动自检日志（两种模式各一行）：

```text
[SECURITY] rate_limit keySource=remoteAddr-only trusted_proxies=[] (X-Real-IP / X-Forwarded-For 一律忽略 —— 直连客户端伪造无效)
[SECURITY] rate_limit keySource=remoteAddr trusted_proxies=[127.0.0.1/32] (仅这些地址转发的 X-Forwarded-For/X-Real-IP 会被解析)
```

### 2.2 RT-2 未捕获异常

```bash
curl -s -D- -X POST http://127.0.0.1:8083/api/v1/appeals/NOPE/process \
     -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"action":"accept","comment":"rt2"}'
```

| | 攻击前（8081 旧构建） | 攻击后（8083 新构建） |
|---|---|---|
| 状态行 | `HTTP/1.1 200` | `HTTP/1.1 500` |
| body | `{"code":500,"message":"系统繁忙…（追踪ID：ec8893d79e8…"}` | `{"code":500,"message":"系统繁忙…（追踪ID：0747c8266996…"}` |
| 响应头 | 无 trace 头 | `X-Trace-Id: 0747c8266996` |
| 服务端日志 | 有 | 有，且与响应同 ID |

```text
2026-10-30 15:16:32.472 [http-nio-0.0.0.0-8083-exec-20] ERROR [c.f.c.e.GlobalExceptionHandler] -
【RuntimeException】非预期异常 | traceId=0747c8266996 | 请求=POST /api/v1/appeals/NOPE/process |
异常类型=java.lang.RuntimeException | 异常消息=申诉不存在
```

### 2.3 RT-3 / RT-4 二次作废与库存回补

夹具：`O-RT7-0001`（`order_status=0, payment_status=0`），两条明细 `food 2 ×2`、`food 3 ×1`，
关联厨房单 `KO-RT7-0001 (pending, material_locked=1)`。

**攻击前**（8081 旧构建，对已作废单 `O1793287678113` 用新幂等键二次 void）：

```text
HTTP 500 | {"code":500,"message":"作废失败：### Error updating database. Cause:
org.postgresql.util.PSQLException: 错误: 重复键违反唯一约束"uk_exception_ops_order"
Detail: Key (order_id)=(O1793287678113) 已经存在 ### SQL: INSERT INTO exception_ops ..."}
```

**攻击后**（8083 新构建）：

```text
BEFORE      : stock2=1051 stock3=893 ops=0 ko=pending   order_status=0
1st void    : HTTP 200 {"code":0,"message":"作废成功","replayed":false}
AFTER  1st  : stock2=1053 stock3=894 ops=1 ko=cancelled order_status=3   ← +2/+1 正好一次
2nd void    : HTTP 409 {"code":409,"message":"订单已是【已取消】状态，无需重复作废"}
AFTER  2nd  : stock2=1053 stock3=894 ops=1 ko=cancelled order_status=3   ← 零二次回补
3rd(replay) : HTTP 409 {"code":409,"message":"订单已是【已取消】状态，无需重复作废"}
AFTER replay: stock2=1053 stock3=894 ops=1 ko=cancelled order_status=3
```

服务端日志（同一次事务内）：

```text
[EXC-VOID] 厨房单已取消 orderId=O-RT7-0001 kitchenOrderId=KO-RT7-0001
[EXC-VOID] 库存回补完成 orderId=O-RT7-0001 回补明细数=2
[METRIC] exception_void_stock_restored orderId=O-RT7-0001 lines=2
[METRIC] exception_void_rejected_cas_miss orderId=O-RT7-0001 orderStatus=3   ← 二次作废走这里
```

---

## 3. 确定性单测（19 passed）

```text
Tests run: 4, Failures: 0 -- GlobalExceptionHandlerTest        (真 500 / 409)
Tests run: 7, Failures: 0 -- RateLimitFilterTest              (伪造头 / 可信代理 / XFF 左段 / 键上限 / 清扫)
Tests run: 4, Failures: 0 -- PosExceptionTxBeanTest           (二次作废 409 / KO 失败回滚 / 唯一键转 409)
Tests run: 2, Failures: 0 -- TtlOrderCloseTxBeanTest          (CAS 0 行不动库存 / CAS 1 行回补)
Tests run: 2, Failures: 0 -- ExceptionOpsMapperSqlContractTest(casCancel 必须含 order_status=0)
```

---

## 4. 运维落地清单

1. **生产 Nginx**：把 Nginx 出口 IP 写进 `rate-limit.trusted-proxies`（如 `10.0.0.10,10.0.0.11`），否则所有请求都会被算成 Nginx 的 IP（宁可"过严"也不可"可伪造"）。
2. **重启**：已把 8081 主实例重启为最终构建（PID 5380，`mvn -o compile` 产物 + 同一 classpath）；8083 为临时验证实例（已停止）；**8082 审计隔离实例仍是旧构建**，需按需重启。
3. **tech-debt SEC-1 仍在**：内存限流只对单实例有效，多实例部署必须换 Redis/网关限流（R7 RT-5 的共享存储部分）。
4. 库存回补口径 = `foods.stock`（POS 下单扣的那一列）；`inventory.locked_quantity` 全仓恒 0，不做假释放。
