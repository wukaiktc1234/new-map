# 会话交接：数据链/UI 优化（2026-08-02 重启点）

> 新会话启动后：先读本文档 → 再读 `docs/data-chain-fix-tracking.md`（批 1~22 详细记录）→ 按"剩余任务"开工。
> 本会话累计完成批 1~22（资金链、采购闭环、UI 大改、多供应商拆分、徽章/拖拽/审批对话框等）。

---

## 0. 环境与验证方法（重要）

| 项 | 值 |
|---|---|
| 前端 dev | `http://localhost:3002`（`frontend/logs/start-frontend.ps1`）；**改样式/变量后需重启 dev 才生效** |
| 后端 | `http://localhost:8081/api`，health `/api/actuator/health`（`backend/logs/start-new-backend.ps1`，改代码后 mvn compile + 重启） |
| 登录 | admin / `Admin@123` |
| 数据库 | PostgreSQL 5432 `food_traceability` postgres/123456；psql 在 `P:\my-new-project\PostgreSQL\18\bin\psql.exe`；**SQL 含中文必须写 UTF8 文件 -f 执行，输出重定向读文件（控制台 GBK 乱码）** |
| 编译 | 后端 `workdir=backend` 下 `mvn compile`；前端 `workdir=frontend` 下 `npm run build` |
| **工具链（全项目内，2026-08-02 起）** | JDK21=`P:\my-new-project\JDK21`；Maven=`P:\my-new-project\tools\apache-maven-3.9.11\bin\mvn.cmd`（**不依赖 H:\fuwu / H:\jdk-25 / 系统 PATH**）；Node=`P:\my-new-project\tools\nodejs`（npm.cmd 同目录）；所有启动脚本已改指向项目内路径（launcher.bat / start-backend.ps1 / start-new-backend.ps1 / start-simple|app.bat / start-dev.bat / start-frontend.ps1 / build-ipa.bat）；调用前设 `$env:JAVA_HOME='P:\my-new-project\JDK21'`。**注意**：`java`/`node`/`npm` 裸命令仍命中系统 PATH（JDK25/H:\nodejs），勿直接使用，一律走脚本或项目内绝对路径 |
| Playwright | 脚本放 `C:\Users\Liberty\AppData\Local\Temp\opencode\*.cjs`，`NODE_PATH=frontend/node_modules` + `channel:'msedge'`；登录=填 admin/Admin@123 点登录；**模型不支持读图，验证用文本断言** |
| **坑1** | PowerShell 在 workdir 下用相对路径会重复前缀——**一律用绝对路径 `P:\my-new-project\...`** |
| **坑2** | `.fts-dialog--lg` 全局强制 960px（会覆盖 width）；宽对话框用 `fts-dialog--xl`(1200)/`fts-dialog--wide`(1100，见 `_element-overrides.scss`) |
| **坑3** | psql 直接 -c 传中文会编码错；改文件用 edit 工具（Read 后精确匹配） |
| **坑4** | 工具链已全部收进项目（`tools/` 下 maven/node，JDK21 根目录）：脚本内显式指向项目内路径；裸 `java`/`npm` 会命中系统 PATH（JDK25/H:\nodejs），不要直接用 |

## 1. 业务链（用户拍板）

```
需求层（非采购部门）: 物资需求提报 /workspace/material-request（OA化：申请人/部门后端绑定）
  → 提交 → 采购申请 /purchase/request（采购部处理：审批→生成订单[跳转定位]）
采购部自用: 采购计划（直接建草稿）→ 审批 → 生成订单
订单: 提交审批→审批→确认下单→收货→到货登记→质检→确认入库
  → 库存(unit_cost最新价)+应付(AP幂等)+追溯码+成本+订单实收
多供应商: 新建订单保存时按明细物料主供应商自动拆多张订单（后端 createOrder 分组）
```

## 2. 本会话已完成的重点（批 1~23 详见 fix-tracking.md）

- 业务链对齐权限模板（批 23）：供应链铁三角 product/purchase/store-ops 全开规则；3 个系统模板 roleConfig 重写（centralized-single 店长=营业+供应链+追溯链、运营补采购/产品/会员；连锁店长=营业链采购归总部）；purchase 菜单 +店长、store-management 菜单 +运营；applyTemplate 链完整性校验（断裂→warn+审计）
- A1 驳回限制定案并实施（批 23）：`purchase_request.reject_count`（V20260802_003）；驳回+1、≥3 禁止提交（code 8106）、`POST /{id}/reset-reject` 管理员重置；前端"已驳回 N/3"标签 + 重提置灰 + admin 重置按钮
- 预设权限模板默认激活修复（批 22，上线 P0）：`permission.active_template` 服务端持久化（system_config）+ 前端登录兜底读取，默认 centralized-single；连带修复 system_config 表契约断裂（create_time/update_time + encrypted boolean）
- 资金链 F1-F6（应收/报销/总账/订单应收/结算联动/资金流水）、D+/E 决策看板、直收移除、申请→订单跳转
- 采购主闭环 7.1：到货单质检/确认入库（库存/应付/追溯码/实收回写/仓库兜底）、转单 500 修复（storeId）
- 权限/业务方向：需求提报统一走采购申请（material_request 下线）、request-center 菜单（工作台域）
- 商品档案：分类过滤、页大小持久化、加列（条码/产地/保质期/存储条件/使用部门 department_id）、序号列
- 订单详情：摘要行+步骤条+Tab（基本信息/明细/审批记录）、步骤条节点弹原组件详情（RequestDetailDialog/ArrivalDetailDialog）、审批记录紧凑表格
- 驳回闭环：REJECTED=7（可修改重新提交）、审批时间/创建人落库、列表操作列状态驱动
- 徽章颜色根治：StatusTag inline style 绑 `--fts-status-*` 变量（tokens+dark 双套，含 default/primary/danger/orange/info 蓝灰）
- 表格拖拽横滚：`vTableDragScroll` 指令（rAF、滚动容器 `.el-scrollbar__wrap`、click 抑制已复位）
- 多供应商拆分：createOrder 分组拆单（实测 2 单）、选物料自动带表头供应商
- 审批对话框：仿详情 status-header（色带+大字+业务单号）
- 编辑重复 bug 根治：`selectByRequestId` SQL 加 `deleted=0`（逻辑删除被查回导致累积）
- 对话框推广：订单/计划 1200、档案/供应商/结算/到货/申请详情 1100、明细表格 cell padding 覆盖、物料下拉富选项+选中短名

## 3. 剩余任务（按优先级）

### A. 用户已确认方案、待实施
1. ~~驳回次数限制~~：已实施（批 23）：reject_count 3 次置灰 + 管理员重置；**单店简化版已定案**（同人自审自动通过 + 资金/删除二次确认 + 审计留痕，数据链不砍只合并操作入口）——待用户确认启动
2. **单店流程简化（批 23 定案，待实施）**：①同人自审自动通过（提交人=审批人时自动通过+审计"自审自动通过"）；②资金（付款/退款/结算）与终止/删除操作二次确认；③到货登记+确认入库合一（质检默认通过可备注）；④开关走 system_config（绑定 centralized-single）
3. **订单终止设计**（已调研给出方案，待实施）：状态机新增"已终止"终态（与"已取消"区分）；已生成应付的订单终止 → 财务模块收到终止标记；已入库/完成不可终止；终止后数据锁定（明细/金额只读）
4. **单元字典（方案B）**：V20260731_010 字典表已建，待接业务约束

### B. 数据链 P0（原有清单，未完成）
4. 会员链 M1-M6：余额分元转换、退款扣余额（addBalance）、统计接口真实聚合、列表分页契约（返 IPage）、状态/性别/渠道编码转换、paymentStatus 补 partial_refunded/success
5. 追溯链 T1-T3/R1-R2：materialId 类型对齐、FoodTraceCode JSON 结构、召回状态统一、PUT body/query 统一
6. 日结 D1-D2：状态枚举对齐、refundAmount/confirmTime
7. 7.2 销售闭环自检（订单→支付→完成→扣库存→成本→资金流水/应收）+ 7.4 会员链自检
8. 端到端验证清单：`docs/data-chain-action-plan.md` 第七章 7.1~7.4 逐条跑

### C. UI 推广（用户要求"慢慢推进"）
9. 对话框/下拉设计推广：采购模块余下页面（采购申请新增/编辑已 1100；分类 680 合理；合同 1200）——到货单/申请详情已推广 1100，后续按需
10. 采购申请"新增/编辑"对话框 = 1100 保持（fts-dialog--lg 已覆盖问题注意）；详情页 RequestDetailDialog 已 1100

## 4. 关键文件

| 文件 | 说明 |
|---|---|
| `docs/data-chain-fix-tracking.md` | 批 1~22 全部修复记录（权威） |
| `docs/data-chain-action-plan.md` | 主计划/状态总览/端到端验证清单 |
| `docs/data-business-chain.md` | 业务链设计 |
| `docs/session-split.md` | 会话分工（窗口A数据链/窗口B复核） |
| `frontend/src/directives/tableDragScroll.ts` | 表格拖拽横滚指令 |
| `frontend/src/components/core/StatusTag.vue` | 徽章（inline style 绑变量） |
| `frontend/src/components/business/ApprovalDialog.vue` / `ApprovalRecordCompact.vue` / `PurchaseRefDialog.vue` / `CrossSupplierTipDialog.vue` | 审批/记录/关联单据/提示组件 |
| `frontend/src/views/purchase/components/` | RequestDetailDialog / ArrivalDetailDialog / ArrivalFormDialog / PurchaseOrderFormDialog / PurchaseTraceDialog(已弃用) |
| `backend/.../mapper/PurchaseRequestItemMapper.java` | selectByRequestId 已加 deleted=0 |
| `backend/.../service/impl/PurchaseOrderServiceImpl.java` | createOrder 多供应商拆分 + createByName 填充 |
| `backend/.../controller/SystemConfigController.java` | GET/PUT /v1/system-config/permission-template（激活模板持久化） |
| `backend/.../entity/SystemConfig.java` | @TableField create_time/update_time 修复（V20260717_020 列统一） |
| `backend/.../resources/db/migration/V20260802_001/002/003` | 默认激活模板 + encrypted 列修复 + 采购申请 reject_count |
| `frontend/src/stores/permission.ts` | initFromToken 服务端激活模板兜底 + applyTemplate 持久化 + validateChainIntegrity 链校验 |
| `backend/.../service/impl/PurchaseRequestServiceImpl.java` | rejectCount 计数/校验 + resetRejectCount（A1） |
| `frontend/src/views/purchase/PurchaseRequest.vue` | "已驳回 N/3"标签 + 重提置灰 + admin 重置驳回按钮 |

## 5. 数据/测试遗留

- 申请 PR20260801006：rejected，明细 2 条（wawa 单位 jian / 午餐肉）——用户可编辑修正（bug 已修，现在编辑保存正常）
- 测试订单：55(approved)/56(pending 曾改)/PO202608020003/004（拆分产物）；申请 PR20260801005（completed）
- 审批记录=审批日志；收货/入库流转看订单详情步骤条（非审批记录）
- 已知孤儿应付 AP0000000007/8/15/16/17（7-28 遗留，可清理）
- admin 用户无部门（departmentId null），提报页按部门过滤时 admin 可见全部通用物料

## 6. 方法论（复验/新任务）

1. 以前端为测试重点：Playwright 驱动真实操作（点击/填表/提交），文本断言 + 后端 API 辅助定位
2. UI 改动后必须重启 dev（scss/tokens 修改不热更）
3. 后端改动：mvn compile → 重启 → health UP → API 验证
4. 业务问题先给行业方案讨论（SAP/用友/钉钉范式）→ 用户确认 → 实施
