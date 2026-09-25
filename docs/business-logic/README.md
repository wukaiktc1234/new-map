# 业务逻辑文档（business-logic）

- **定位**：业务"应该怎么工作"的权威描述——与治理文档（怎么做工程）互补
- **建立方式**：AI 按代码与活体验证整理 → **Owner 审定后成为权威**；未审定前视为"AI 理解稿"
- **使用流程**：出问题 → 先查本文档对照实现 → 文档错修文档 / 代码错修代码 → 两边改完回归验证
- **维护纪律**：修一条链，落/更一份文档；不批量补、不逐函数写

## 索引

| 文档 | 覆盖链 | 验证状态 |
|------|--------|----------|
| [01-procurement.md](01-procurement.md) | 采购：申请→订单→到货→收货→库存 | ✅ 2026-09-25 活体全链 |
| [02-pos-order.md](02-pos-order.md) | POS：菜单→下单→支付→KDS→扣料 | ✅ 2026-09-25 活体全链 |
| [03-foods-recipes.md](03-foods-recipes.md) | 商品/配方：建档→配方→同步 | ✅ 2026-09-25 活体 |
| [04-units-conventions.md](04-units-conventions.md) | 单位与金额约定（斤/克/分） | ✅ 2026-09-25 实测换算 |
| 02-inventory.md（库存调拨/盘点） | 未建 | ⏳ 待该链修复/走查时补 |
| 04-finance.md（财务凭证链） | 未建 | ⏳ 当前仅见 finance_records 写入，未成链验证 |

## 已知问题索引（详见各文档"已知问题"节）

| 编号 | 摘要 | 状态 |
|------|------|------|
| F4 | 采购订单供应商绑定：物料档案主供应商优先，表头 supplierId 仅 fallback | 待修复卡（根因诊断 `docs/quality/f4-supplier-binding-diagnosis-001.md`） |
| F6 | 新建菜品不同步 legacy food 表 → POS 下单报误导性"库存不足"，需重启 | 待修复卡 |
| F5 | 采购收货入总仓 inventory 行 product_name/unit 为 NULL | 待修复 |
| F2 | 配方无独立管理端点（只能随菜品创建） | 待产品裁决 |
| F1/F3/F7 | 原料模板 templateCode 不自动生成 / 枚举中英混排 / 0 元财务流水 | 待排期 |

## 证据

- 活体走查：`docs/quality/business-chain-verification-20260925.md`（2026-09-25，七步全链）
