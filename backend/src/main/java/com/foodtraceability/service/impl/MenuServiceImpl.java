package com.foodtraceability.service.impl;

import com.foodtraceability.dto.MenuResponse;
import com.foodtraceability.dto.MenuMeta;
import com.foodtraceability.service.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuServiceImpl.class);

    @Override
    public MenuResponse getMenus(String userId) {
        logger.info("获取用户菜单: userId={}", userId);
        MenuResponse response = new MenuResponse();
        List<MenuResponse.MenuItem> menus = new ArrayList<>();

        menus.add(buildMenu("Home", "首页", "/home", null, null, "home-filled", 1, 1, "system"));

        menus.add(buildMenuWithChildren("ProductManagement", "产品中心", "/product", null, "/product/list", "goods", 0, 2, "product",
                buildChild("ProductList", "菜品管理", "/product/list", "product:manage", "food", 1),
                buildChild("CategoryManagement", "分类管理", "/product/category", "product:manage", "category", 2),
                buildChild("DishCombo", "套餐管理", "/product/combo", "product:manage", "collection-tag", 3),
                buildChild("ProductPricing", "产品定价", "/product/pricing", "product:manage", "currency-circle", 4),
                buildChild("ProductBOM", "配方管理(BOM)", "/product/bom", "product:bom:manage", "connection", 5),
                buildChild("ProductSalesAnalysis", "菜品成本分析", "/product/sales-analysis", "product:view", "trend-charts", 6)
        ));

        menus.add(buildMenuWithChildren("OrderManagement", "订单中心", "/order", null, "/order/center", "operation", 0, 3, "order",
                buildChild("OrderCenter", "订单中心", "/order/center", "order:manage", "search", 1),
                buildChild("OrderReservation", "预订单管理", "/order/reservation", "order:manage", "calendar", 2),
                buildChild("OrderChainSummary", "连锁订单汇总", "/order/chain-summary", "order:view", "data-analysis", 3)
        ));

        menus.add(buildMenuWithChildren("WarehouseManagement", "仓储管理", "/warehouse", null, "/warehouse/overview", "box", 0, 4, "inventory",
                buildChild("WarehouseOverview", "库存概览", "/warehouse/overview", "warehouse:overview:view", "data-board", 1),
                buildChild("WarehouseInventory", "库存管理", "/warehouse/inventory", "warehouse:inventory:view", "box", 2),
                buildChild("InventoryInbound", "入库管理", "/warehouse/inbound", "warehouse:inbound:manage", "download", 3),
                buildChild("InventoryOutbound", "出库管理", "/warehouse/outbound", "warehouse:outbound:manage", "reduce-stock", 4),
                buildChild("InventoryWarning", "库存预警", "/warehouse/warning", "warehouse:warning:manage", "warning", 5),
                buildChild("InventoryReport", "库存报表", "/warehouse/report", "warehouse:report:view", "document", 6),
                buildChild("InventoryLocation", "库位管理", "/warehouse/location", "warehouse:location:manage", "grid", 7),
                buildChild("InventoryCheck", "库存盘点", "/warehouse/check", "warehouse:check:manage", "check", 8),
                buildChild("InventoryTransfer", "库存调拨", "/warehouse/transfer", "warehouse:transfer:manage", "exchange", 9),
                buildChild("InventoryLog", "库存日志", "/warehouse/log", "warehouse:log:view", "document", 10),
                buildChild("WarehouseSettings", "仓库设置", "/warehouse/settings", "warehouse:settings:manage", "setting", 11),
                buildChild("StoreInventory", "门店库存", "/warehouse/store-inventory", "warehouse:store:manage", "shop", 12)
        ));

        menus.add(buildMenuWithChildren("PurchaseManagement", "采购管理", "/purchase", null, "/purchase/overview", "shopping-cart", 0, 5, "purchase",
                buildChild("PurchaseOverview", "采购概览", "/purchase/overview", "purchase:overview:view", "data-board", 1),
                buildChild("MaterialArchive", "商品档案", "/purchase/archive", "purchase:archive:manage", "files", 2),
                buildChild("MaterialCategoryManage", "商品分类", "/purchase/material-category", "purchase:category:manage", "folder", 3),
                buildChild("PurchaseRequest", "采购申请", "/purchase/request", "purchase:request:manage", "edit", 4),
                buildChild("PurchasePlan", "采购计划", "/purchase/plan", "purchase:plan:manage", "calendar", 5),
                buildChild("PurchaseOrderList", "采购订单", "/purchase/orders", "purchase:order:manage", "document", 6),
                buildChild("PurchaseStockin", "采购入库", "/purchase/stockin", "purchase:stockin:manage", "box", 7),
                buildChild("PurchaseReturn", "采购退货", "/purchase/return", "purchase:return:manage", "refresh-left", 8),
                buildChild("PurchaseSupplier", "供应商管理", "/purchase/supplier", "purchase:supplier:manage", "office-building", 9),
                buildChild("PurchaseReport", "采购报表", "/purchase/report", "purchase:report:view", "data-analysis", 10),
                buildChild("PurchaseSettlement", "采购结算", "/purchase/settlement", "purchase:settlement:manage", "money", 11),
                buildChild("PurchaseContractList", "采购合同", "/purchase/contract", "purchase:contract:manage", "document-copy", 12),
                buildChild("ElectronicContractList", "电子合同", "/purchase/electronic-contract", "purchase:contract:manage", "document-checked", 13)
        ));

        // [V3.2] 生产加工模块已移至后厨端独立应用，管理端不再显示
        // 后厨端功能: KDS订单看板、托盘工作台(内置)、制作记录、原料消耗、厨房统计
        // 管理端可通过"数据决策-厨房效率分析"查看汇总数据

        menus.add(buildMenuWithChildren("DeviceManagement", "设备管理", "/device", null, "/device/list", "monitor", 0, 15, "device",
                buildChild("DeviceList", "设备列表", "/device/list", "device:list:view", "monitor", 1),
                buildChild("PrintTaskManager", "打印任务队列", "/device/print-tasks", "device:print:manage", "printer", 2),
                buildChild("DeviceAlertCenter", "告警中心", "/device/alerts", "device:alert:view", "warning", 3),
                buildChild("DeviceMonitor", "状态监控大屏", "/device/monitor", "device:monitor:view", "data-board", 4),
                buildChild("DeviceSettings", "设备参数设置", "/device/settings", "device:settings:manage", "setting", 5)
                // [V3.2] 设备模拟器已隐藏(menuType=2)，仅开发模式可见
                // buildChild("DeviceSimulator", "设备模拟器", "/device/simulator", "device:simulator:operate", "magic-stick", 5)
        ));

        menus.add(buildMenuWithChildren("AssetManagement", "资产管理", "/asset", null, "/asset/overview", "coin", 0, 16, "asset",
                buildChild("AssetOverview", "资产概览", "/asset/overview", "asset:view", "data-board", 1),
                buildChild("AssetLedger", "资产台账", "/asset/ledger", "asset:manage", "document", 2),
                buildChild("AssetCategory", "资产分类", "/asset/category", "asset:manage", "folder", 3),
                buildChild("AssetDepreciation", "资产折旧", "/asset/depreciation", "asset:finance", "trend-charts", 4),
                buildChild("AssetInventory", "资产盘点", "/asset/inventory", "asset:inventory", "check", 5)
                // [V3.2] 托盘管理已移至后厨端KDS内置组件，不再作为独立菜单
        ));

        menus.add(buildMenuWithChildren("MarketingManagement", "营销中心", "/marketing", null, "/marketing/overview", "shopping-bag", 0, 17, "marketing",
                buildChild("MarketingOverview", "营销概览", "/marketing/overview", "marketing:view", "data-board", 1),
                buildChild("MarketingPopular", "爆品分析", "/marketing/popular", "marketing:view", "top", 2),
                buildChild("MarketingCustomer", "顾客分析", "/marketing/customer", "marketing:view", "user", 3),
                buildChild("MarketingStatistics", "营销数据", "/marketing/statistics", "marketing:view", "data-board", 4),
                buildChild("MarketingMemberSystem", "会员管理", "/marketing/member-system", "marketing:manage", "user-filled", 5),
                buildChild("MarketingPromotion", "活动管理", "/marketing/promotion", "marketing:manage", "promotion", 6),
                buildChild("MarketingCoupon", "优惠券", "/marketing/coupon", "marketing:manage", "ticket", 7),
                buildChild("MarketingActivityAnalysis", "活动效果", "/marketing/activity-analysis", "marketing:view", "data-analysis", 8),
                buildChild("MarketingPlatformSetting", "外卖平台设置", "/marketing/platform-setting", "marketing:manage", "setting", 9)
        ));

        // [V3.2] 门店运营重构为"事务处理中心"
        // 定位: 店长的日常办公桌，信息汇聚与事务处理节点
        // 核心理念: 只看各端状态/记录，不做经营操作（叫号/桌台等在收银端）
        menus.add(buildMenuWithChildren("StoreOperations", "门店运营", "/store-ops", null, "/store-ops/pending", "monitor", 0, 18, "store",
                buildChild("StorePendingTasks", "待办事务箱", "/store-ops/pending-tasks", "store:pending:manage", "bell", 1),
                buildChild("StoreStatusOverview", "门店状态总览", "/store-ops/status-overview", "store:status:view", "data-board", 2),
                buildChild("StoreOperationLog", "门店日志", "/store-ops/operation-log", "store:log:view", "document", 3),
                buildChild("StoreDailySettlement", "日结对账", "/store-ops/daily-settlement", "store:settlement:manage", "money", 4),
                buildChild("StoreAnnouncement", "门店公告", "/store-ops/announcement", "store:announcement:manage", "notification", 5),
                buildChild("StoreCertificateManagement", "证件管理", "/store-ops/certificate", "store:certificate:manage", "medal", 6),
                buildChild("StoreRecruitment", "门店招聘", "/store-ops/recruitment", "store:recruitment:manage", "user-filled", 7)
                // [V3.2] 已移除的功能(归属其他端或模块):
                // - 叫号管理 → 收银端应用
                // - 健康证管理 → 此处保留证件展示(原件保管)，人事模块负责全局管控
                // - 交易查询 → 订单中心(已有完整功能)
                // - 退款处理 → 订单中心(已有完整功能)
                // - 班次管理 → 可保留或移至系统设置
                // - 支付配置 → 系统设置
                // - 门店资产 → 资产管理模块
        ));

        menus.add(buildMenuWithChildren("AnalyticsManagement", "数据决策", "/analytics", null, "/analytics/dashboard", "data-analysis", 0, 19, "analytics",
                buildChild("AnalyticsDashboard", "经营驾驶舱", "/analytics/dashboard", "analytics:view", "data-board", 1),
                buildChild("AnalyticsSales", "销售分析", "/analytics/sales", "analytics:view", "trend-charts", 2),
                buildChild("AnalyticsInventory", "库存分析", "/analytics/inventory", "analytics:view", "box", 3),
                buildChild("AnalyticsCost", "成本分析", "/analytics/cost", "analytics:view", "trend-charts", 4),
                buildChild("AnalyticsCustomer", "客户分析", "/analytics/customer", "analytics:view", "user", 5),
                buildChild("AnalyticsEfficiency", "人效分析", "/analytics/efficiency", "analytics:view", "user-filled", 6),
                buildChild("AnalyticsKitchenEfficiency", "厨房效率分析", "/analytics/kitchen-efficiency", "analytics:kitchen:view", "food", 7),
                buildChild("AnalyticsCustomReport", "自定义报表", "/analytics/custom-report", "analytics:manage", "document", 8)
        ));

        menus.add(buildMenuWithChildren("TraceabilityManagement", "食品追溯", "/traceability", null, "/traceability/query/summary", "location", 0, 20, "traceability",
                buildChild("TraceabilitySummary", "追溯查询", "/traceability/query", null, "search", 1),
                buildChild("TraceabilitySupplier", "供应商追溯", "/traceability/supplier", null, "office-building", 2),
                buildChild("TraceabilityQuality", "质量追溯", "/traceability/quality", null, "quality", 3),
                buildChild("TraceabilityInspection", "检验记录", "/traceability/inspection", null, "document", 4),
                buildChild("MaterialTraceCode", "原料追溯码", "/traceability/material-code", null, "tickets", 5),
                buildChild("FoodTraceCode", "食品追溯码", "/traceability/food-trace-code", null, "food", 6),
                buildChild("TraceChainView", "追溯链展示", "/traceability/chain", null, "link", 7),
                buildChild("LabelTemplateDesign", "标签模板设计", "/traceability/label-template", null, "postcard", 8)
        ));

        menus.add(buildMenuWithChildren("FinanceManagement", "财务中心", "/finance", null, "/finance/ledger", "money", 0, 21, "finance",
                buildChild("FinanceLedger", "财务总账", "/finance/ledger", "finance:view", "notebook", 1),
                buildChild("FinanceReceivable", "应收账款", "/finance/receivable", "finance:manage", "wallet", 2),
                buildChild("FinancePayable", "应付账款", "/finance/payable", "finance:manage", "wallet", 3),
                // [V3.2] 资产管理已移至独立资产管理模块，此处删除避免重复
                // buildChild("FinanceAsset", "资产管理", "/finance/asset", "finance:manage", "coin", 4),
                buildChild("FinanceFund", "资金管理", "/finance/fund", "finance:manage", "wallet", 4),
                buildChild("FinanceCost", "成本管理", "/finance/cost", "finance:manage", "trend-charts", 5),
                buildChild("FinanceBudget", "预算管理", "/finance/budget", "finance:manage", "calendar", 6),
                buildChild("FinanceTax", "税务管理", "/finance/tax", "finance:manage", "calculator", 7),
                buildChild("ElectronicVoucher", "电子凭证管理", "/finance/electronic-voucher", "finance:manage", "document-checked", 8),
                buildChild("InvoiceReimbursement", "发票报销", "/finance/invoice-reimbursement", "finance:manage", "money", 9),
                buildChild("FinanceReport", "财务报表", "/finance/report", "finance:manage", "document", 10),
                buildChild("FinanceApproval", "财务审批", "/finance/approval", "finance:manage", "stamp", 11),
                buildChild("FinanceRisk", "财务风险", "/finance/risk", "finance:manage", "warning", 12)
        ));

        menus.add(buildMenuWithChildren("HRManagement", "人事管理", "/hr", null, "/hr/employee", "users", 0, 22, "hr",
                buildChild("HREmployee", "员工管理", "/hr/employee", "hr:manage", "user", 1),
                buildChild("HROrganization", "组织管理", "/hr/organization", "hr:manage", "office-building", 2),
                buildChild("HRAttendance", "考勤管理", "/hr/attendance", "hr:manage", "clock", 3),
                buildChild("HRSalary", "薪资管理", "/hr/salary", "hr:manage", "money", 4),
                buildChild("HRRecruitment", "招聘管理", "/hr/recruitment", "hr:manage", "user-filled", 5),
                buildChild("HROnboarding", "入职办理", "/hr/onboarding", "hr:manage", "user", 6),
                buildChild("HRHealthCertificate", "健康证管理", "/hr/health-certificate", "hr:manage", "medal", 7),
                buildChild("HRTraining", "培训管理", "/hr/training", "hr:manage", "reading", 8),
                buildChild("HRAnalytics", "人事分析", "/hr/analytics", "hr:manage", "data-analysis", 9),
                buildChild("HRApproval", "审批管理", "/hr/approval", "hr:manage", "check", 10),
                buildChild("HRInvitationCode", "邀请码管理", "/hr/invitation-code", "hr:manage", "ticket", 11),
                buildChild("HRLaborContract", "合同管理", "/hr/contract", "hr:manage", "document", 12)
        ));

        menus.add(buildMenuWithChildren("SystemSettings", "系统设置", "/system", null, "/system/settings", "setting", 0, 13, "system",
                buildChild("SystemSettingsPage", "系统配置", "/system/settings", "system:manage", "setting", 1),
                buildChild("OperationLog", "操作日志", "/system/log", "system:manage", "document", 2)
        ));

        menus.add(buildMenu("TableManagement", "桌台管理", "/tables", null, null, "grid", 1, 23, "order"));
        menus.add(buildMenu("PrintQueueManagement", "打印队列", "/print-queue", null, null, "printer", 1, 24, "system"));

        response.setMenus(menus);
        logger.info("菜单数据构建完成: userId={}, 菜单数量={}", userId, menus.size());
        return response;
    }

    private MenuResponse.MenuItem buildMenu(String id, String name, String path,
            String component, String redirect, String icon, int menuType, int sort, String category) {
        MenuResponse.MenuItem item = new MenuResponse.MenuItem();
        item.setId(id);
        item.setName(name);
        item.setPath(path);
        item.setComponent(component);
        item.setRedirect(redirect);
        item.setIcon(icon);
        item.setMenuType(menuType);
        item.setSort(sort);

        MenuMeta meta = new MenuMeta();
        meta.setTitle(name);
        meta.setIcon(icon);
        meta.setMenuType(menuType);
        meta.setCategory(category);
        meta.setRequireAuth(true);
        item.setMeta(meta);

        return item;
    }

    private MenuResponse.MenuItem buildMenuWithChildren(String id, String name, String path,
            String component, String redirect, String icon, int menuType, int sort, String category,
            MenuResponse.MenuItem... children) {
        MenuResponse.MenuItem item = buildMenu(id, name, path, component, redirect, icon, menuType, sort, category);
        item.setChildren(Arrays.asList(children));
        return item;
    }

    private MenuResponse.MenuItem buildChild(String id, String name, String path,
            String permission, String icon, int sort) {
        MenuResponse.MenuItem item = new MenuResponse.MenuItem();
        item.setId(id);
        item.setName(name);
        item.setPath(path);
        item.setIcon(icon);
        item.setMenuType(1);
        item.setSort(sort);
        item.setPermission(permission);

        MenuMeta meta = new MenuMeta();
        meta.setTitle(name);
        meta.setIcon(icon);
        meta.setMenuType(1);
        meta.setRequireAuth(true);
        if (permission != null) {
            meta.setPermissions(new String[]{permission});
        }
        item.setMeta(meta);

        return item;
    }
}
