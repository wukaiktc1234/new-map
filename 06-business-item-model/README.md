# 业务项模型分析

## 项目概述

本目录包含对餐饮系统业务项模型的深度分析，旨在解决系统中多个"物"概念（Food、Product、Material）的关系问题。

## 分析目标

1. 理解当前系统中 Food、Product、Material 的关系
2. 确定库存管理的核心对象
3. 分析同一业务对象在不同上下文中的角色
4. 提出统一的业务项模型建议

## 核心发现

- 系统存在三个独立的"物"概念：Food、Product（已废弃）、Material
- 库存管理的是 `material_id`
- 同一业务对象（如"可乐"）可在不同上下文中承担不同角色
- 推荐采用混合领域模型（Hybrid Domain Model）

## 文件说明

| 文件 | 说明 |
|------|------|
| `business-item-model-summary.md` | 详细的分析总结，包含核心发现、问题回答和推荐模型 |
| `business-role-model.md` | 业务角色模型分析 |
| `candidate-model-comparison.md` | 候选模型对比分析 |
| `canonical-identity-analysis.md` | 规范身份分析 |
| `inventory-stockable-model.md` | 库存可存储模型分析 |
| `procurement-model.md` | 采购模型分析 |
| `product-food-material-boundary-analysis.md` | Product-Food-Material 边界分析 |
| `recipe-bom-model.md` | 配方/BOM 模型分析 |
| `sales-pos-model.md` | 销售/POS 模型分析 |
| `store-scope-analysis.md` | 门店范围分析 |
| `unit-uom-analysis.md` | 单位/UOM 分析 |

## 分析方法

- 只读分析，不修改任何代码
- 基于现有数据库表结构分析
- 业务场景角色映射

## 状态

**只读分析任务** - 仅提供分析结果和建议，不涉及代码修改。

## 日期

2026-09-09