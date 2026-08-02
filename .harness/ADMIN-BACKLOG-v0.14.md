# v0.14.0 管理端待办事项清单

> 生成时间: 2026-04-07
> 生成原因: v0.14.0 本窗口专注于收银端/后厨端/叫号系统，管理端任务交由另一窗口执行
> 前置依赖: v0.13.1 全部修复已完成（P0/P1/Critical/E2E全通过）

---

## 一、会员管理体系建设（高优先级）

### 1.1 数据库设计
需新建以下表：

#### member_points_log (积分流水表)
| 字段 | 类型 | 说明 |
|------|------|------|
| point_id | BIGINT PK | 流水ID |
| member_no | VARCHAR(32) | 会员编号 |
| points | INTEGER | 积分变动(正=获得/负=消耗) |
| balance_after | INTEGER | 变动后余额 |
| source | VARCHAR(32) | 来源(consumption/refund/recharge/admin) |
| ref_order_id | VARCHAR(64) | 关联订单ID |
| create_time | DATETIME | 创建时间 |

#### member_level (会员等级表)
| 字段 | 类型 | 说明 |
|------|------|------|
| level_id | INT PK | 等级ID |
| level_name | VARCHAR(32) | 等级名称(普通/银卡/金卡/钻石) |
| min_points | INT | 最低积分 |
| discount_rate | DECIMAL(5,2) | 折扣率(如0.95=95折) |
| points_rate | DECIMAL(5,2) | 积分倍率(如1.5=1.5倍) |

#### 修改 members 表
新增字段: total_points(总积分), level_id(等级ID), total_consumption(累计消费), last_consume_time(最后消费时间)

### 1.2 管理端页面
- [ ] 会员列表页（CRUD + 搜索/筛选/分页）
- [ ] 会员详情页（基本信息/消费记录/积分历史/充值记录）
- [ ] 会员等级配置页（等级规则/折扣率/积分倍率）
- [ ] 积分调整页（管理员手动调整积分+原因）

### 1.3 后端API
- [ ] MemberPointService: 积分计算/查询/调整
- [ ] MemberLevelService: 等级判定/升级降级
- [ ] MemberController: 会员CRUD + 积分 + 等级 相关REST API

### 1.4 小程序联动
- [ ] 小程序会员页调用统一会员API
- [ ] 积分查看/使用/历史
- [ ] 余额查询/充值记录

---

## 二、报表中心（高优先级）

### 2.1 需要的报表
| 报表名称 | 维度 | 数据源 |
|---------|------|--------|
| 营业日报 | 日/周/月 订单数+金额+客单价 | orders |
| 热销菜品TOP10 | 销量排行 | order_items JOIN food |
| 时段分析 | 每小时订单分布 | orders.create_time |
| 分类销售占比 | 菜品分类销售额 | order_items JOIN food.category |
| 支付方式分布 | 微信/现金/...占比 | orders.payment_method |
| 退款分析 | 退款率/退款原因 | orders WHERE status=refund |

### 2.2 管理端页面
- [ ] 报表首页（Dashboard式关键指标卡片）
- [ ] 各报表独立页面（表格+图表）
- [ ] 日期范围选择器
- [ ] 导出Excel/PDF

### 2.3 后端API
- [ ] ReportController: /api/v1/reports/*
- [ ] ReportService: SQL聚合查询
- [ ] 可考虑使用 existing KitchenStatsDTO 模式扩展

---

## 三、排班管理（中优先级）

### 3.1 数据库
staff_schedule 表: staff_id, shift_date, shift_type(早/中/晚/休), start_time, end_time, status

### 3.2 功能
- [ ] 排班日历视图
- [ ] 员工排班CRUD
- [ ] 排班冲突检测
- [ ] 出勤打卡（可选）

---

## 四、RabbitMQ消息队列启用（中优先级）

### 4.1 当前状态
- pom.xml 有 amqp 依赖
- application.yml 有 RabbitMQ 配置但可能被禁用
- 需确认哪些业务场景应使用异步消息

### 4.2 建议启用场景
- [ ] 订单创建 → 库存扣减（异步解耦）
- [ ] 支付完成 → 积分累加（异步）
- [ ] 厨房完成 → 叫号通知（已有WebSocket，MQ可作为备选）
- [ ] 发送通知（短信/微信模板消息）

---

## 五、其他配套（低优先级）

| # | 项目 | 说明 |
|---|------|------|
| 5.1 | 库存管理完整版 | 入库/出库/盘点/预警/调拨（管理端F4完整版） |
| 5.2 | 支付回调→积分自动化 | PayCallbackHandler 支付成功后自动计算积分 |
| 5.3 | 多终端余额同步 | 充值后Redis发布事件，各终端订阅更新 |
| 5.4 | 定时任务完善 | 除OrderTimeoutTask外还需哪些定时任务？ |

---

## 六、执行建议顺序

1. **第一批（核心业务闭环）**: 会员体系 + 报表中心
2. **第二批（运营效率）**: 排班 + RabbitMQ + 库存完整版
3. **第三批（体验优化）**: 自动化链路 + 多终端同步

---

## 七、验收标准参考

每个管理端功能完成后应验证：
- [ ] 页面正常打开（HTTP 200）
- [ ] CRUD操作正确（Create/Read/Update/Delete）
- [ ] 权限控制有效（@PreAuthorize）
- [ ] 前后端构建通过（mvn compile + npm run build）
- [ ] E2E关键流程测试通过
