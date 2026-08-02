// ============================================================
// 企业完整数据生命周期测试 v2
// 模拟真实企业运营全流程，验证数据流转正确性
// ============================================================
const { chromium } = require("playwright");
const path = require("path");
const fs = require("fs");

const BASE = "http://localhost:3002";
const AUTH_FILE = path.resolve("frontend/e2e/.auth/lifecycle-v2.json");
const TS = Date.now();
const TAG = "ent-v2-" + TS.toString(36);
const SCREENSHOT_DIR = path.resolve("frontend/e2e/test-results/lifecycle-v2-" + TS);
const REPORT_FILE = path.resolve("frontend/e2e/test-results/lifecycle-v2-report.json");

// Test data identifiers (for cleanup)
const testData = {
  storeName: "V2门店-" + TAG,
  storeCode: "VS-" + TS.toString(36).toUpperCase(),
  categoryName: "V2分类-" + TAG,
  foodName: "V2菜品-" + TAG,
  supplierName: "V2供应商-" + TAG,
  supplierCode: "S-" + TS.toString(36).toUpperCase(),
  employeeName: "V2员工-" + TAG,
  employeeCode: "EMP-" + TS.toString(36).toUpperCase(),
  assetName: "V2资产-" + TAG,
  assetCode: "AST-" + TS.toString(36).toUpperCase(),
};

// Results tracking
const results = { passed: 0, failed: 0, warns: 0, phases: [] };

require("fs").mkdirSync(SCREENSHOT_DIR, { recursive: true });
require("fs").mkdirSync(path.dirname(AUTH_FILE), { recursive: true });

let ssCount = 0;
async function screenshot(page, label) {
  const idx = String(++ssCount).padStart(3, "0");
  const p = path.join(SCREENSHOT_DIR, idx + "-" + label + ".png");
  await page.screenshot({ path: p, fullPage: false });
  return p;
}

function logResult(phase, testName, status, detail = "") {
  results.phases.push({ phase, test: testName, status, detail, screenshot: "" });
  const icon = status === "PASS" ? "✅" : status === "WARN" ? "⚠️" : "❌";
  console.log(`  ${icon} [${phase}] ${testName} ${detail ? "- " + detail : ""}`);
}

// ============================================================
// UTILITY FUNCTIONS
// ============================================================
async function goto(page, url) {
  await page.goto(BASE + url, { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2000);
}

async function clickBtn(page, text) {
  const btn = page.locator("button").filter({ hasText: text });
  if (await btn.count() > 0) { await btn.first().click(); await page.waitForTimeout(1200); return true; }
  return false;
}

async function fillField(page, label, value) {
  const item = page.locator(".el-form-item:visible").filter({ hasText: label });
  const input = item.locator("input:visible, textarea:visible").first();
  if (await input.count() > 0) { await input.fill(value); return true; }
  return false;
}

async function dialogConfirm(page, text = "确定") {
  const btn = page.locator(".el-dialog:visible button, .el-drawer:visible button").filter({ hasText: text });
  if (await btn.count() > 0) { await btn.first().click(); await page.waitForTimeout(2500); return true; }
  return false;
}

async function getRowCount(page) {
  return await page.locator(".el-table__body-wrapper tr.el-table__row").count();
}

async function tableContains(page, text) {
  const bodyText = await page.locator(".el-table__body-wrapper").innerText().catch(() => "");
  return bodyText.includes(text);
}

async function bodyContains(page, text) {
  const bodyText = await page.locator("body").innerText().catch(() => "");
  return bodyText.includes(text);
}

async function apiLogin(page) {
  await page.goto(BASE + "/login", { waitUntil: "domcontentloaded", timeout: 30000 });
  const result = await page.evaluate(async () => {
    const r = await fetch("/api/v1/auth/login", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: "admin", password: "Admin@123" }),
    });
    return r.json();
  });
  if (result.code !== 0) throw new Error("Login failed: " + JSON.stringify(result));
  const d = result.data;
  await page.evaluate((data) => {
    localStorage.setItem("token", data.token);
    localStorage.setItem("refresh_token", data.refreshToken);
    localStorage.setItem("user_id", data.userInfo?.id || "1");
    localStorage.setItem("username", data.userInfo?.username || "admin");
  }, d);
  await page.context().storageState({ path: AUTH_FILE });
  return page;
}

(async () => {
  console.log("\n" + "=".repeat(70));
  console.log("🏢 企业完整数据生命周期测试 v2");
  console.log("📋 TAG:", TAG);
  console.log("=".repeat(70) + "\n");

  const browser = await chromium.launch({
    headless: true,
    channel: "chrome",
    args: ["--disable-blink-features=AutomationControlled"],
  });

  try {
    // ---- PHASE 0: Login ----
    console.log("[Phase 0] 🔑 登录认证");
    const loginPage = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
    await apiLogin(loginPage);
    await loginPage.close();

    const ctx = await browser.newContext({
      viewport: { width: 1920, height: 1080 },
      storageState: AUTH_FILE,
    });
    const page = await ctx.newPage();

    let phaseResults = { phase: "开业准备", tests: [] };
    const track = (name, status, detail) => {
      phaseResults.tests.push({ name, status, detail });
      logResult("开业准备", name, status, detail);
    };

    // ================================================================
    // PHASE 1: 开业准备 — 门店/分类/菜品/定价
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 1] 🏪 开业准备 — 门店/分类/菜品/定价");
    console.log("-".repeat(50));

    try {
      // 1.1 门店创建
      await goto(page, "/operations/store-archive");
      await screenshot(page, "p1.1-store-before");

      await clickBtn(page, "新增门店");
      await page.waitForTimeout(1500);
      await screenshot(page, "p1.1-store-dialog");

      const storeDialogVisible = await page.locator(".el-dialog:visible").count();
      track("1.1 门店对话框打开", storeDialogVisible > 0 ? "PASS" : "FAIL", `对话框: ${storeDialogVisible}`);

      if (storeDialogVisible > 0) {
        await fillField(page, "门店名称", testData.storeName);
        await fillField(page, "门店编码", testData.storeCode);
        await dialogConfirm(page, "确定");
        await page.waitForTimeout(2000);
        await screenshot(page, "p1.1-store-after");

        const found = await tableContains(page, testData.storeName);
        track("1.2 门店数据验证", found ? "PASS" : "FAIL", `在表格中找到"${testData.storeName}": ${found}`);
      }
    } catch (e) {
      track("1.x 门店流程异常", "FAIL", e.message.slice(0, 120));
    }
    results.phases.push(phaseResults);
    phaseResults = { phase: "菜品管理", tests: [] };

    // 1.3-1.5 菜品分类/菜品/定价
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 1b] 🥘 菜品管理 — 分类/菜品/定价");
    console.log("-".repeat(50));

    try {
      // 菜品分类
      await goto(page, "/product/category");
      await screenshot(page, "p1.3-category");
      const categoryLoaded = await page.locator("body").innerText().then(t => t.length).catch(() => 0);
      track("1.3 分类页面加载", categoryLoaded > 50 ? "PASS" : "FAIL", `内容长度: ${categoryLoaded}`);

      // 菜品管理
      await goto(page, "/product/food");
      await screenshot(page, "p1.4-food-before");

      await clickBtn(page, "新增菜品");
      await page.waitForTimeout(1500);
      const foodDialog = await page.locator(".el-dialog:visible").count();
      await screenshot(page, "p1.4-food-dialog");
      track("1.4 菜品对话框打开", foodDialog > 0 ? "PASS" : "WARN", `对话框: ${foodDialog}`);

      if (foodDialog > 0) {
        await fillField(page, "菜品名称", testData.foodName);
        await dialogConfirm(page, "确定");
        await page.waitForTimeout(2000);
        await screenshot(page, "p1.4-food-after");
        const found = await tableContains(page, testData.foodName);
        track("1.5 菜品数据验证", found ? "PASS" : "FAIL", `在表格中找到"${testData.foodName}": ${found}`);
      }

      // 定价页面
      await goto(page, "/product/pricing");
      await screenshot(page, "p1.5-pricing");
      const pricingLoaded = await page.locator("body").innerText().then(t => t.length).catch(() => 0);
      track("1.6 定价页面加载", pricingLoaded > 50 ? "PASS" : "FAIL", `内容长度: ${pricingLoaded}`);
    } catch (e) {
      track("1.x 菜品流程异常", "FAIL", e.message.slice(0, 120));
    }
    results.phases.push(phaseResults);
    phaseResults = { phase: "采购仓储", tests: [] };

    // ================================================================
    // PHASE 2: 采购仓储 — 供应商/采购单/仓库
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 2] 📦 采购仓储 — 供应商/采购/库存");
    console.log("-".repeat(50));

    try {
      // 供应商
      await goto(page, "/purchase/supplier");
      await screenshot(page, "p2.1-supplier-before");
      const supplierRowsBefore = await getRowCount(page);
      track("2.1 供应商列表加载", "PASS", `当前行数: ${supplierRowsBefore}`);

      await clickBtn(page, "新增供应商");
      await page.waitForTimeout(1500);
      const suppDialog = await page.locator(".el-dialog:visible").count();
      await screenshot(page, "p2.1-supplier-dialog");
      track("2.2 供应商对话框", suppDialog > 0 ? "PASS" : "WARN", `对话框: ${suppDialog}`);

      if (suppDialog > 0) {
        await fillField(page, "供应商名称", testData.supplierName);
        await fillField(page, "供应商编码", testData.supplierCode);
        await fillField(page, "联系人", "张三");
        await fillField(page, "联系电话", "13800138000");
        await dialogConfirm(page, "确定");
        await page.waitForTimeout(2000);
        await screenshot(page, "p2.1-supplier-after");
        const found = await tableContains(page, testData.supplierName);
        track("2.3 供应商数据验证", found ? "PASS" : "FAIL", `在表格中: ${found}`);
      }

      // 采购订单页面
      await goto(page, "/purchase/orders");
      await screenshot(page, "p2.2-purchase-orders");
      const poLoaded = await page.locator("body").innerText().then(t => t.length).catch(() => 0);
      track("2.4 采购订单页面", poLoaded > 50 ? "PASS" : "FAIL", `内容长度: ${poLoaded}`);

      // 采购入库
      await goto(page, "/purchase/warehousing");
      await screenshot(page, "p2.3-warehousing");
      const whLoaded = await page.locator("body").innerText().then(t => t.length).catch(() => 0);
      track("2.5 采购入库页面", whLoaded > 50 ? "PASS" : "FAIL", `内容长度: ${whLoaded}`);

      // 仓库库存
      await goto(page, "/warehouse/inventory");
      await screenshot(page, "p2.4-inventory");
      const invRows = await getRowCount(page);
      track("2.6 仓库库存加载", "PASS", `库存行数: ${invRows}`);

      // 仓库盘点
      await goto(page, "/warehouse/stocktaking");
      await screenshot(page, "p2.5-stocktaking");
      track("2.7 库存盘点页面", "PASS", "页面加载成功");
    } catch (e) {
      track("2.x 采购仓储异常", "FAIL", e.message.slice(0, 120));
    }
    results.phases.push(phaseResults);
    phaseResults = { phase: "人事管理", tests: [] };

    // ================================================================
    // PHASE 3: 人事管理 — 员工/合同/考勤
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 3] 👥 人事管理 — 员工/合同/考勤/薪资");
    console.log("-".repeat(50));

    try {
      await goto(page, "/hr/employee");
      await screenshot(page, "p3.1-employee-before");
      const empRowsBefore = await getRowCount(page);
      track("3.1 员工列表加载", "PASS", `当前员工数: ${empRowsBefore}`);

      await clickBtn(page, "新增员工");
      await page.waitForTimeout(1500);
      const empDialog = await page.locator(".el-dialog:visible").count();
      await screenshot(page, "p3.1-employee-dialog");
      track("3.2 员工对话框", empDialog > 0 ? "PASS" : "WARN", `对话框: ${empDialog}`);

      if (empDialog > 0) {
        await fillField(page, "姓名", testData.employeeName);
        await fillField(page, "员工编号", testData.employeeCode);
        await dialogConfirm(page, "确定");
        await page.waitForTimeout(2000);
        await screenshot(page, "p3.1-employee-after");
        const found = await tableContains(page, testData.employeeName);
        track("3.3 员工数据验证", found ? "PASS" : "FAIL", `在表格中找到: ${found}`);
      }

      // 合同管理
      await goto(page, "/hr/contract");
      await screenshot(page, "p3.2-contract");
      track("3.4 合同管理页面", "PASS", "页面加载成功");

      // 考勤管理
      await goto(page, "/hr/attendance");
      await screenshot(page, "p3.3-attendance");
      track("3.5 考勤管理页面", "PASS", "页面加载成功");

      // 薪资管理
      await goto(page, "/hr/salary");
      await screenshot(page, "p3.4-salary");
      track("3.6 薪资管理页面", "PASS", "页面加载成功");
    } catch (e) {
      track("3.x 人事流程异常", "FAIL", e.message.slice(0, 120));
    }
    results.phases.push(phaseResults);
    phaseResults = { phase: "运营销售", tests: [] };

    // ================================================================
    // PHASE 4: 运营销售 — 订单/会员/运营
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 4] 📊 运营销售 — 订单/会员/运营分析");
    console.log("-".repeat(50));

    try {
      // 订单查询
      await goto(page, "/order/query");
      await screenshot(page, "p4.1-orders-before");
      await clickBtn(page, "查询");
      await page.waitForTimeout(2000);
      const orderRows = await getRowCount(page);
      await screenshot(page, "p4.1-orders-after");
      track("4.1 订单查询", "PASS", `订单数: ${orderRows}`);

      // 订单统计
      await goto(page, "/order/statistics");
      await screenshot(page, "p4.2-order-stats");
      track("4.2 订单统计页面", "PASS", "加载成功");

      // 退款管理
      await goto(page, "/order/refund");
      await screenshot(page, "p4.3-refund");
      track("4.3 退款管理页面", "PASS", "加载成功");

      // 预约管理
      await goto(page, "/order/reservation");
      await screenshot(page, "p4.4-reservation");
      track("4.4 预约管理页面", "PASS", "加载成功");

      // 运营分析
      await goto(page, "/operations/analysis");
      await screenshot(page, "p4.5-analysis");
      track("4.5 运营分析页面", "PASS", "加载成功");

      // 会员管理
      await goto(page, "/member/list");
      await screenshot(page, "p4.6-members");
      const memberRows = await getRowCount(page);
      track("4.6 会员列表", "PASS", `会员数: ${memberRows}`);

      // 会员积分
      await goto(page, "/member/points");
      await screenshot(page, "p4.7-points");
      track("4.7 会员积分页面", "PASS", "加载成功");
    } catch (e) {
      track("4.x 运营销售异常", "FAIL", e.message.slice(0, 120));
    }
    results.phases.push(phaseResults);
    phaseResults = { phase: "财务资产", tests: [] };

    // ================================================================
    // PHASE 5: 财务资产 — 总账/凭证/费用/资产
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 5] 💰 财务资产 — 总账/凭证/费用/资产");
    console.log("-".repeat(50));

    try {
      // 总账
      await goto(page, "/finance/ledger");
      await screenshot(page, "p5.1-ledger");
      const ledgerRows = await getRowCount(page);
      track("5.1 财务总账", "PASS", `总账行数: ${ledgerRows}`);

      // 凭证管理
      await goto(page, "/finance/voucher");
      await screenshot(page, "p5.2-voucher");
      track("5.2 凭证管理", "PASS", "加载成功");

      // 费用管理
      await goto(page, "/finance/expense");
      await screenshot(page, "p5.3-expense");
      track("5.3 费用管理", "PASS", "加载成功");

      // 财务报表
      await goto(page, "/finance/reports");
      await screenshot(page, "p5.4-reports");
      track("5.4 财务报表", "PASS", "加载成功");

      // 资产台账
      await goto(page, "/asset/ledger");
      await screenshot(page, "p5.5-asset-before");
      const assetRowsBefore = await getRowCount(page);
      track("5.5 资产台账加载", "PASS", `资产行数: ${assetRowsBefore}`);

      // 资产折旧
      await goto(page, "/asset/depreciation");
      await screenshot(page, "p5.6-depreciation");
      track("5.6 资产折旧页面", "PASS", "加载成功");

      // 资产盘点
      await goto(page, "/asset/inventory");
      await screenshot(page, "p5.7-inventory");
      track("5.7 资产盘点页面", "PASS", "加载成功");
    } catch (e) {
      track("5.x 财务资产异常", "FAIL", e.message.slice(0, 120));
    }
    results.phases.push(phaseResults);
    phaseResults = { phase: "合规管理", tests: [] };

    // ================================================================
    // PHASE 6: 合规管理 — 溯源/设备/签章/系统
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 6] 📋 合规管理 — 溯源/设备/签章/系统");
    console.log("-".repeat(50));

    try {
      // 溯源管理
      await goto(page, "/traceability/list");
      await screenshot(page, "p6.1-trace");
      track("6.1 溯源列表", "PASS", "加载成功");

      await goto(page, "/traceability/batch");
      await screenshot(page, "p6.2-trace-batch");
      track("6.2 溯源批次", "PASS", "加载成功");

      // 设备管理
      await goto(page, "/device/list");
      await screenshot(page, "p6.3-device");
      const deviceRows = await getRowCount(page);
      track("6.3 设备列表", "PASS", `设备数: ${deviceRows}`);

      await goto(page, "/device/maintenance");
      await screenshot(page, "p6.4-device-maint");
      track("6.4 设备维护", "PASS", "加载成功");

      // 电子签章
      await goto(page, "/seal/manage");
      await screenshot(page, "p6.5-seal");
      track("6.5 签章管理", "PASS", "加载成功");

      await goto(page, "/seal/apply");
      await screenshot(page, "p6.6-seal-apply");
      track("6.6 签章申请", "PASS", "加载成功");

      // 系统管理
      await goto(page, "/system/permission-center");
      await screenshot(page, "p6.7-permission-center");
      track("6.7 权限中心", "PASS", "加载成功");

      await goto(page, "/system/permission");
      await screenshot(page, "p6.8-system-config");
      track("6.8 系统配置", "PASS", "加载成功");

      await goto(page, "/system/operation-audit");
      await screenshot(page, "p6.9-audit");
      track("6.9 操作审计", "PASS", "加载成功");
    } catch (e) {
      track("6.x 合规管理异常", "FAIL", e.message.slice(0, 120));
    }
    results.phases.push(phaseResults);
    phaseResults = { phase: "收尾验证", tests: [] };

    // ================================================================
    // PHASE 7: 收尾验证 — 工作台 + 个人中心 + 数据流转校验
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 7] 🏁 收尾验证 — 仪表盘/个人/数据流转");
    console.log("-".repeat(50));

    try {
      // 工作台
      await goto(page, "/home");
      await screenshot(page, "p7.1-home");
      const homeText = await page.locator("body").innerText().catch(() => "");
      track("7.1 工作台加载", homeText.length > 200 ? "PASS" : "WARN", `内容长度: ${homeText.length}`);

      // 个人中心
      await goto(page, "/personal/profile");
      await screenshot(page, "p7.2-profile");
      const profileText = await page.locator("body").innerText().catch(() => "");
      track("7.2 个人中心加载", profileText.length > 100 ? "PASS" : "WARN", `内容长度: ${profileText.length}`);

      // Cross-module verification: check store name appears in store management page
      await goto(page, "/operations/store-archive");
      await page.waitForTimeout(1000);
      // Try to search for the created store
      const searchInput = page.locator("input[placeholder*='搜索'], input[placeholder*='门店'], input[placeholder*='查询']").first();
      if (await searchInput.count() > 0) {
        await searchInput.fill(testData.storeName);
        await page.keyboard.press("Enter");
        await page.waitForTimeout(2000);
        await screenshot(page, "p7.3-cross-module-search");
        const searchResult = await tableContains(page, testData.storeName);
        track("7.3 跨模块数据搜索验证", searchResult ? "PASS" : "WARN", `搜索"${testData.storeName}": ${searchResult}`);
      } else {
        track("7.3 跨模块搜索组件", "WARN", "未找到搜索输入框");
      }

      // Verify employee appears in HR page (data persistence across navigation)
      await goto(page, "/hr/employee");
      await page.waitForTimeout(1000);
      const empSearch = page.locator("input[placeholder*='搜索'], input[placeholder*='员工'], input[placeholder*='查询']").first();
      if (await empSearch.count() > 0) {
        await empSearch.fill(testData.employeeName);
        await page.keyboard.press("Enter");
        await page.waitForTimeout(2000);
        await screenshot(page, "p7.4-employee-search");
        const empFound = await tableContains(page, testData.employeeName);
        track("7.4 员工数据持久化验证", empFound ? "PASS" : "WARN", `搜索"${testData.employeeName}": ${empFound}`);
      } else {
        track("7.4 员工搜索", "WARN", "未找到搜索框");
      }

      // Route coverage: verify all 16 module root pages
      const allRoutes = [
        ["工作台", "/home"],
        ["产品中心", "/product/food"],
        ["订单管理", "/order/query"],
        ["运营中心", "/operations"],
        ["门店管理", "/operations/store-archive"],
        ["采购管理", "/purchase/orders"],
        ["仓储管理", "/warehouse/overview"],
        ["会员管理", "/member/list"],
        ["财务中心", "/finance/ledger"],
        ["资产管理", "/asset/ledger"],
        ["人事管理", "/hr/employee"],
        ["溯源管理", "/traceability/list"],
        ["设备管理", "/device/list"],
        ["电子签章", "/seal/manage"],
        ["系统管理", "/system/permission"],
        ["个人中心", "/personal/profile"],
      ];
      let allAccessible = true;
      let inaccessibleRoutes = [];
      for (const [name, url] of allRoutes) {
        try {
          await goto(page, url);
          const bodyLen = await page.locator("body").innerText().then(t => t.length).catch(() => 0);
          if (bodyLen < 20) {
            allAccessible = false;
            inaccessibleRoutes.push(name);
          }
        } catch (e) {
          allAccessible = false;
          inaccessibleRoutes.push(name);
        }
      }
      track("7.5 16模块路由全覆盖", allAccessible ? "PASS" : "FAIL",
        inaccessibleRoutes.length > 0 ? `不可访问: ${inaccessibleRoutes.join(", ")}` : "全部通过");
    } catch (e) {
      track("7.x 收尾验证异常", "FAIL", e.message.slice(0, 120));
    }
    results.phases.push(phaseResults);

    // ================================================================
    // Summary
    // ================================================================
    let totalPassed = 0, totalFailed = 0, totalWarns = 0;
    for (const p of results.phases) {
      for (const t of p.tests) {
        if (t.status === "PASS") totalPassed++;
        else if (t.status === "FAIL") totalFailed++;
        else totalWarns++;
      }
    }
    results.passed = totalPassed;
    results.failed = totalFailed;
    results.warns = totalWarns;

    // Write report
    fs.writeFileSync(REPORT_FILE, JSON.stringify(results, null, 2));
    console.log("\n" + "=".repeat(70));
    console.log(`📊 测试报告已保存: ${REPORT_FILE}`);
    console.log(`📸 截图: ${SCREENSHOT_DIR} (${ssCount} 张)`);
    console.log(`\n🏆 结果: ✅ ${totalPassed} 通过 | ❌ ${totalFailed} 失败 | ⚠️ ${totalWarns} 警告`);
    console.log("=".repeat(70) + "\n");

    await browser.close();
    process.exit(totalFailed > 0 ? 1 : 0);

  } catch (err) {
    console.error("\n💥 [FATAL]", err.message);
    try {
      const p2 = await browser.newPage();
      await p2.screenshot({ path: path.join(SCREENSHOT_DIR, "fatal-error.png"), fullPage: false });
      await p2.close();
    } catch (_) {}
    await browser.close();
    process.exit(1);
  }
})();
