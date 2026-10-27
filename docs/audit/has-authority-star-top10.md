# `hasAuthority('*')` Top 10 模块排序（2026-10-28）

## 排序口径与数据来源（重要：说明局限）

Lead 要求按「近 30 天前端调用 / 日志流量」排序。**实际能拿到的数据只有两类**：

| 数据源 | 可得性 | 结论 |
|--------|--------|------|
| 生产访问日志 / APM | **不可得**（无集中日志系统，`ApiResponseTimeAspect` 只打本地日志） | 无法按真实流量排序 |
| 本次会话本地日志 | 可得，但**只有我自己的验证请求**（login 9 次等），不代表业务流量 | 不可用作排序依据 |
| **前端 API 引用数**（grep `frontend/src/**/*.ts`） | 可得、可复现 | **采用**：反映「前端有多少地方真的会调这个模块」 |

→ 所以下面的排序是「**前端引用热度**」，不是「真实调用量」。要真实流量需要：网关 access log 或 APM（建议 V2 补）。

## Top 10（按前端引用数）

| # | 模块 | 前端引用数 | `hasAuthority('*')` 处数 | 判定 |
|---|------|-----------|--------------------------|------|
| 1 | **资产**（`asset`） | 112 | 15 | **本批改造** |
| 2 | **设备**（`device`） | 38 | 53 | **本批改造** |
| 3 | **订单**（`order`） | 33 | 18 | **本批改造** |
| 4 | **标签打印**（`label`） | 31 | 30 | 维持 admin 专用 |
| 5 | **印章**（`seal`） | 20 | 9 | 维持 admin 专用 |
| 6 | **会员**（`member`） | 18 | 23 | 维持 admin 专用 |
| 7 | **托盘**（`tray`） | 15 | 10 | 维持 admin 专用 |
| 8 | **审批**（`approval`） | 15 | 9 | 维持 admin 专用 |
| 9 | **AI 模型配置**（`ai-model`） | 13 | 10 | 维持 admin 专用 |
| 10 | **积分**（`points`） | 10 | 9 | 维持 admin 专用 |

## Top 3 改造清单（Lead 批准先做这三个）

| 模块 | 待改 Controller | 处数 | 需补权限码 |
|------|-----------------|------|------------|
| 资产 | `AssetController` | 15 | `asset:view` / `asset:manage`（需核对是否已有等价码） |
| 设备 | `DeviceController` 12 + `DeviceAlertController` 15 + `DeviceManagementController` 11 + `DeviceCommunicationTestController` 11 | 49 | `device:view/manage`、`device:alert:view/manage` |
| 订单 | `OrderNewController` | 18 | `order:view` / `order:manage` |

**其余 79 个 Controller / 剩余 ~455 处：维持 admin 专用**，在审计文档统一标注：

> **当前仅 admin 可调用，属已知限制（不是 bug）** —— `hasAuthority('*')` 依赖 `SecurityUser:73` 为 admin 硬加的 `*` authority；
> 其余角色因 permissions 表不存在 `'*'` 行而无法进入。细化需要**产品参与制定权限矩阵**（模块 × 动作 × 角色），
> 在此之前逐处改码 = 在没有矩阵的情况下臆造权限边界。
