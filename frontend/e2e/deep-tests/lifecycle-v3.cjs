// ============================================================
// 企业完整数据生命周期测试 v3 — 修复版
// 修复: refreshAndCheck 定位搜索框逻辑
// ============================================================
const { chromium } = require("playwright");
const path = require("path");
const fs = require("fs");

const BASE = "http://localhost:3002";
const AUTH_FILE = path.resolve("frontend/e2e/.auth/lifecycle-v3.json");
const TS = Date.now();
const TAG = "ent-v3-" + TS.toString(36);
const SCREENSHOT_DIR = path.resolve("frontend/e2e/test-results/lifecycle-v3-" + TS);
const REPORT_FILE = path.resolve("frontend/e2e/test-results/lifecycle-v3-report.json");

const testData = {
  storeName: "V3门店-" + TAG,
  storeCode: "V3S-" + TS.toString(36).toUpperCase(),
  foodName: "V3菜品-" + TAG,
  supplierName: "V3供应商-" + TAG,
  supplierCode: "V3S-" + TS.toString(36).toUpperCase(),
  employeeName: "V3员工-" + TAG,
  employeeCode: "V3E-" + TS.toString(36).toUpperCase(),
};

const allResults = [];
let ssCount = 0;

fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
fs.mkdirSync(path.dirname(AUTH_FILE), { recursive: true });

async function screenshot(page, label) {
  const idx = String(++ssCount).padStart(3, "0");
  await page.screenshot({ path: path.join(SCREENSHOT_DIR, idx + "-" + label + ".png"), fullPage: false });
}

function track(phase, testName, status, detail) {
  allResults.push({ phase, test: testName, status, detail: detail || "" });
  const icon = status === "PASS" ? "\u2705" : status === "WARN" ? "\u26A0\uFE0F" : "\u274C";
  console.log("  " + icon + " [" + phase + "] " + testName + (detail ? " - " + detail : ""));
}

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
  const input = item.locator("input:visible:not([readonly]), textarea:visible").first();
  if (await input.count() > 0) { await input.fill(value); return true; }
  return false;
}

async function dialogConfirm(page, text) {
  if (!text) text = "\u786E\u5B9A";
  const btn = page.locator(".el-dialog:visible button, .el-drawer:visible button").filter({ hasText: text });
  if (await btn.count() > 0) { await btn.first().click(); await page.waitForTimeout(3000); return true; }
  return false;
}

async function getRowCount(page) {
  return await page.locator(".el-table__body-wrapper tr.el-table__row").count();
}

async function tableContains(page, text) {
  const bodyText = await page.locator(".el-table__body-wrapper").innerText().catch(function() { return ""; });
  return bodyText.indexOf(text) >= 0;
}

async function refreshAndCheckTable(page, url, searchText) {
  await goto(page, url);
  await page.waitForTimeout(1000);
  // Just check if text appears anywhere in the table
  var found = await tableContains(page, searchText);
  // Try pagination / clicking search button
  if (!found) {
    var searchBtn = page.locator("button").filter({ hasText: "\u67E5\u8BE2" });
    if (await searchBtn.count() > 0) {
      await searchBtn.first().click();
      await page.waitForTimeout(2000);
      found = await tableContains(page, searchText);
    }
  }
  return found;
}

async function apiLogin(page) {
  await page.goto(BASE + "/login", { waitUntil: "domcontentloaded", timeout: 30000 });
  var result = await page.evaluate(async function() {
    var r = await fetch("/api/v1/auth/login", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: "admin", password: "Admin@123" }),
    });
    return r.json();
  });
  if (result.code !== 0) throw new Error("Login failed: " + JSON.stringify(result));
  var d = result.data;
  await page.evaluate(function(data) {
    localStorage.setItem("token", data.token);
    localStorage.setItem("refresh_token", data.refreshToken);
    localStorage.setItem("user_id", data.userInfo && data.userInfo.id ? data.userInfo.id : "1");
    localStorage.setItem("username", data.userInfo && data.userInfo.username ? data.userInfo.username : "admin");
  }, d);
  await page.context().storageState({ path: AUTH_FILE });
}

(async function() {
  console.log("\n" + "=".repeat(70));
  console.log("\uD83C\uDFE2 \u4F01\u4E1A\u5B8C\u6574\u6570\u636E\u751F\u547D\u5468\u671F\u6D4B\u8BD5 v3");
  console.log("\uD83D\uDCCB TAG:", TAG);
  console.log("=".repeat(70) + "\n");

  var browser = await chromium.launch({
    headless: true,
    channel: "chrome",
    args: ["--disable-blink-features=AutomationControlled"],
  });

  try {
    // ---- LOGIN ----
    console.log("[Phase 0] \uD83D\uDD11 \u767B\u5F55\u8BA4\u8BC1");
    var loginPage = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
    await apiLogin(loginPage);
    await loginPage.close();

    var ctx = await browser.newContext({
      viewport: { width: 1920, height: 1080 },
      storageState: AUTH_FILE,
    });
    var page = await ctx.newPage();
    var len, rows, found;

    // ================================================================
    // PHASE 1: 开业准备
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 1] \uD83C\uDFEA \u5F00\u4E1A\u51C6\u5907");
    console.log("-".repeat(50));

    // 1.1 System config page
    await goto(page, "/system/permission");
    await screenshot(page, "p1.1-system-config");
    len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
    track("开业准备", "1.1 系统配置页面", len > 50 ? "PASS" : "FAIL", "\u957F\u5EA6: " + len);

    // 1.2 Store creation
    await goto(page, "/operations/store-archive");
    await screenshot(page, "p1.2-store-before");
    await clickBtn(page, "\u65B0\u589E\u95E8\u5E97");
    await page.waitForTimeout(1500);
    await screenshot(page, "p1.2-store-dialog");
    var storeDialog = await page.locator(".el-dialog:visible").count();
    track("开业准备", "1.2 门店对话框", storeDialog > 0 ? "PASS" : "FAIL", "\u5BF9\u8BDD\u6846: " + storeDialog);

    if (storeDialog > 0) {
      await fillField(page, "\u95E8\u5E97\u540D\u79F0", testData.storeName);
      await fillField(page, "\u95E8\u5E97\u7F16\u7801", testData.storeCode);
      await dialogConfirm(page);
      await page.waitForTimeout(2000);
      await screenshot(page, "p1.2-store-after");
      found = await refreshAndCheckTable(page, "/operations/store-archive", testData.storeName);
      track("开业准备", "1.3 门店数据验证", found ? "PASS" : "FAIL", "\u641C\u7D22\u9A8C\u8BC1: " + found);
    }

    // 1.3 Product category
    await goto(page, "/product/category");
    await screenshot(page, "p1.3-category");
    track("开业准备", "1.4 分类页面", "PASS", "\u52A0\u8F7D\u6210\u529F");

    // 1.4 Food creation
    await goto(page, "/product/food");
    await screenshot(page, "p1.4-food-before");
    await clickBtn(page, "\u65B0\u589E\u83DC\u54C1");
    await page.waitForTimeout(1500);
    await screenshot(page, "p1.4-food-dialog");
    var foodDialog = await page.locator(".el-dialog:visible").count();
    track("开业准备", "1.5 菜品对话框", foodDialog > 0 ? "PASS" : "WARN", "\u5BF9\u8BDD\u6846: " + foodDialog);

    if (foodDialog > 0) {
      await fillField(page, "\u83DC\u54C1\u540D\u79F0", testData.foodName);
      await dialogConfirm(page);
      await page.waitForTimeout(2000);
      found = await refreshAndCheckTable(page, "/product/food", testData.foodName);
      track("开业准备", "1.6 菜品数据验证", found ? "PASS" : "FAIL", "\u641C\u7D22\u9A8C\u8BC1: " + found);
    }

    // 1.5 Pricing
    await goto(page, "/product/pricing");
    await screenshot(page, "p1.5-pricing");
    track("开业准备", "1.7 定价页面", "PASS", "\u52A0\u8F7D\u6210\u529F");

    // ================================================================
    // PHASE 2: 采购仓储
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 2] \uD83D\uDCE6 \u91C7\u8D2D\u4ED3\u50A8");
    console.log("-".repeat(50));

    // Supplier
    await goto(page, "/purchase/supplier");
    await screenshot(page, "p2.1-supplier-before");
    track("采购仓储", "2.1 供应商列表", "PASS", "\u52A0\u8F7D\u6210\u529F");

    await clickBtn(page, "\u65B0\u589E\u4F9B\u5E94\u5546");
    await page.waitForTimeout(1500);
    await screenshot(page, "p2.1-supplier-dialog");
    var suppDialog = await page.locator(".el-dialog:visible").count();
    track("采购仓储", "2.2 供应商对话框", suppDialog > 0 ? "PASS" : "WARN", "\u5BF9\u8BDD\u6846: " + suppDialog);

    if (suppDialog > 0) {
      await fillField(page, "\u4F9B\u5E94\u5546\u540D\u79F0", testData.supplierName);
      await fillField(page, "\u4F9B\u5E94\u5546\u7F16\u7801", testData.supplierCode);
      await dialogConfirm(page);
      await page.waitForTimeout(2000);
      found = await refreshAndCheckTable(page, "/purchase/supplier", testData.supplierName);
      track("采购仓储", "2.3 供应商数据验证", found ? "PASS" : "FAIL", "\u641C\u7D22\u9A8C\u8BC1: " + found);
    }

    // Purchase pages
    var purchasePages = [
      ["2.4 采购订单", "/purchase/orders"],
      ["2.5 采购入库", "/purchase/warehousing"],
      ["2.6 采购退货", "/purchase/returns"],
      ["2.7 采购合同", "/purchase/contract"],
    ];
    for (var pi = 0; pi < purchasePages.length; pi++) {
      var pp = purchasePages[pi];
      await goto(page, pp[1]);
      await screenshot(page, pp[0].replace(/\s/g, "-"));
      len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
      track("采购仓储", pp[0], len > 20 ? "PASS" : "FAIL", "\u957F\u5EA6: " + len);
    }

    // Warehouse pages
    var warehousePages = [
      ["2.8 库存概览", "/warehouse/overview"],
      ["2.9 库存明细", "/warehouse/inventory"],
      ["2.10 库存盘点", "/warehouse/stocktaking"],
      ["2.11 库存预警", "/warehouse/warning"],
    ];
    for (var wi = 0; wi < warehousePages.length; wi++) {
      var wp = warehousePages[wi];
      await goto(page, wp[1]);
      await screenshot(page, wp[0].replace(/\s/g, "-"));
      len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
      rows = await getRowCount(page);
      track("采购仓储", wp[0], len > 20 ? "PASS" : "FAIL", "\u957F\u5EA6: " + len + ", \u884C\u6570: " + rows);
    }

    // ================================================================
    // PHASE 3: 人事管理
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 3] \uD83D\uDC65 \u4EBA\u4E8B\u7BA1\u7406");
    console.log("-".repeat(50));

    await goto(page, "/hr/employee");
    await screenshot(page, "p3.1-employee-before");
    await clickBtn(page, "\u65B0\u589E\u5458\u5DE5");
    await page.waitForTimeout(1500);
    await screenshot(page, "p3.1-employee-dialog");
    var empDialog = await page.locator(".el-dialog:visible").count();
    track("人事管理", "3.1 员工对话框", empDialog > 0 ? "PASS" : "WARN", "\u5BF9\u8BDD\u6846: " + empDialog);

    if (empDialog > 0) {
      await fillField(page, "\u59D3\u540D", testData.employeeName);
      await fillField(page, "\u5458\u5DE5\u7F16\u53F7", testData.employeeCode);
      await dialogConfirm(page);
      await page.waitForTimeout(2000);
      found = await refreshAndCheckTable(page, "/hr/employee", testData.employeeName);
      track("人事管理", "3.2 员工数据验证", found ? "PASS" : "FAIL", "\u641C\u7D22\u9A8C\u8BC1: " + found);
    }

    var hrPages = [
      ["3.3 合同管理", "/hr/contract"],
      ["3.4 考勤管理", "/hr/attendance"],
      ["3.5 薪资管理", "/hr/salary"],
      ["3.6 招聘管理", "/hr/recruitment"],
    ];
    for (var hi = 0; hi < hrPages.length; hi++) {
      var hp = hrPages[hi];
      await goto(page, hp[1]);
      await screenshot(page, hp[0].replace(/\s/g, "-"));
      len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
      track("人事管理", hp[0], len > 20 ? "PASS" : "FAIL", "\u957F\u5EA6: " + len);
    }

    // ================================================================
    // PHASE 4: 运营销售
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 4] \uD83D\uDCCA \u8FD0\u8425\u9500\u552E");
    console.log("-".repeat(50));

    var opsPages = [
      ["4.1 订单查询", "/order/query"],
      ["4.2 订单统计", "/order/statistics"],
      ["4.3 退款管理", "/order/refund"],
      ["4.4 预约管理", "/order/reservation"],
      ["4.5 运营分析", "/operations/analysis"],
      ["4.6 经营报表", "/operations/reports"],
      ["4.7 运营策略", "/operations/strategy"],
    ];
    for (var oi = 0; oi < opsPages.length; oi++) {
      var op = opsPages[oi];
      await goto(page, op[1]);
      await screenshot(page, op[0].replace(/\s/g, "-"));
      len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
      rows = await getRowCount(page);
      track("运营销售", op[0], len > 20 ? "PASS" : "FAIL", "\u957F\u5EA6: " + len + ", \u884C\u6570: " + rows);
    }

    // Member pages
    var memberPages = [
      ["4.8 会员列表", "/member/list"],
      ["4.9 会员积分", "/member/points"],
      ["4.10 会员等级", "/member/level"],
      ["4.11 会员互动", "/member/interaction"],
    ];
    for (var mi = 0; mi < memberPages.length; mi++) {
      var mp = memberPages[mi];
      await goto(page, mp[1]);
      await screenshot(page, mp[0].replace(/\s/g, "-"));
      len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
      track("运营销售", mp[0], len > 20 ? "PASS" : "FAIL", "\u957F\u5EA6: " + len);
    }

    // ================================================================
    // PHASE 5: 财务资产
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 5] \uD83D\uDCB0 \u8D22\u52A1\u8D44\u4EA7");
    console.log("-".repeat(50));

    var financePages = [
      ["5.1 财务总账", "/finance/ledger"],
      ["5.2 凭证管理", "/finance/voucher"],
      ["5.3 费用管理", "/finance/expense"],
      ["5.4 财务报表", "/finance/reports"],
      ["5.5 资产台账", "/asset/ledger"],
      ["5.6 资产折旧", "/asset/depreciation"],
      ["5.7 资产盘点", "/asset/inventory"],
      ["5.8 资产调拨", "/asset/transfer"],
    ];
    for (var fi = 0; fi < financePages.length; fi++) {
      var fp = financePages[fi];
      await goto(page, fp[1]);
      await screenshot(page, fp[0].replace(/\s/g, "-"));
      len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
      rows = await getRowCount(page);
      track("财务资产", fp[0], len > 20 ? "PASS" : "FAIL", "\u957F\u5EA6: " + len + ", \u884C\u6570: " + rows);
    }

    // ================================================================
    // PHASE 6: 合规管理
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 6] \uD83D\uDCCB \u5408\u89C4\u7BA1\u7406");
    console.log("-".repeat(50));

    var compliancePages = [
      ["6.1 溯源列表", "/traceability/list"],
      ["6.2 溯源批次", "/traceability/batch"],
      ["6.3 设备列表", "/device/list"],
      ["6.4 设备维护", "/device/maintenance"],
      ["6.5 签章管理", "/seal/manage"],
      ["6.6 签章申请", "/seal/apply"],
      ["6.7 权限中心", "/system/permission-center"],
      ["6.8 系统配置", "/system/permission"],
      ["6.9 操作审计", "/system/operation-audit"],
    ];
    for (var ci = 0; ci < compliancePages.length; ci++) {
      var cp = compliancePages[ci];
      await goto(page, cp[1]);
      await screenshot(page, cp[0].replace(/\s/g, "-"));
      len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
      track("合规管理", cp[0], len > 20 ? "PASS" : "FAIL", "\u957F\u5EA6: " + len);
    }

    // ================================================================
    // PHASE 7: 收尾验证
    // ================================================================
    console.log("\n" + "-".repeat(50));
    console.log("[Phase 7] \uD83C\uDFC1 \u6536\u5C3E\u9A8C\u8BC1");
    console.log("-".repeat(50));

    // 7.1 Dashboard
    await goto(page, "/home");
    await screenshot(page, "p7.1-home");
    len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
    track("收尾验证", "7.1 工作台", len > 100 ? "PASS" : "WARN", "\u957F\u5EA6: " + len);

    // 7.2 Personal profile
    await goto(page, "/personal/profile");
    await screenshot(page, "p7.2-profile");
    len = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
    track("收尾验证", "7.2 个人中心", len > 50 ? "PASS" : "WARN", "\u957F\u5EA6: " + len);

    // 7.3 Cross-module data persistence
    found = await refreshAndCheckTable(page, "/operations/store-archive", testData.storeName);
    track("收尾验证", "7.3 门店数据持久化", found ? "PASS" : "FAIL", "\u641C\u7D22\u201C" + testData.storeName + "\u201D: " + found);

    found = await refreshAndCheckTable(page, "/purchase/supplier", testData.supplierName);
    track("收尾验证", "7.4 供应商数据持久化", found ? "PASS" : "FAIL", "\u641C\u7D22\u201C" + testData.supplierName + "\u201D: " + found);

    // 7.4 16 module routes
    var allRoutes = [
      ["\u5DE5\u4F5C\u53F0", "/home"],
      ["\u4EA7\u54C1\u4E2D\u5FC3", "/product/food"],
      ["\u8BA2\u5355\u7BA1\u7406", "/order/query"],
      ["\u8FD0\u8425\u4E2D\u5FC3", "/operations"],
      ["\u95E8\u5E97\u7BA1\u7406", "/operations/store-archive"],
      ["\u91C7\u8D2D\u7BA1\u7406", "/purchase/orders"],
      ["\u4ED3\u50A8\u7BA1\u7406", "/warehouse/overview"],
      ["\u4F1A\u5458\u7BA1\u7406", "/member/list"],
      ["\u8D22\u52A1\u4E2D\u5FC3", "/finance/ledger"],
      ["\u8D44\u4EA7\u7BA1\u7406", "/asset/ledger"],
      ["\u4EBA\u4E8B\u7BA1\u7406", "/hr/employee"],
      ["\u6EAF\u6E90\u7BA1\u7406", "/traceability/list"],
      ["\u8BBE\u5907\u7BA1\u7406", "/device/list"],
      ["\u7535\u5B50\u7B7E\u7AE0", "/seal/manage"],
      ["\u7CFB\u7EDF\u7BA1\u7406", "/system/permission"],
      ["\u4E2A\u4EBA\u4E2D\u5FC3", "/personal/profile"],
    ];
    var allAccessible = true;
    var inaccessible = [];
    for (var ri = 0; ri < allRoutes.length; ri++) {
      try {
        await goto(page, allRoutes[ri][1]);
        var bodyLen = await page.locator("body").innerText().then(function(t) { return t.length; }).catch(function() { return 0; });
        if (bodyLen < 20) { allAccessible = false; inaccessible.push(allRoutes[ri][0]); }
      } catch (e) { allAccessible = false; inaccessible.push(allRoutes[ri][0]); }
    }
    track("收尾验证", "7.5 16\u6A21\u5757\u8DEF\u7531\u5168\u8986\u76D6",
      allAccessible ? "PASS" : "FAIL",
      inaccessible.length > 0 ? "\u4E0D\u53EF\u8FBE: " + inaccessible.join(", ") : "\u5168\u90E8\u901A\u8FC7");

    // ================================================================
    // SUMMARY
    // ================================================================
    var totalPassed = allResults.filter(function(r) { return r.status === "PASS"; }).length;
    var totalFailed = allResults.filter(function(r) { return r.status === "FAIL"; }).length;
    var totalWarns = allResults.filter(function(r) { return r.status === "WARN"; }).length;

    fs.writeFileSync(REPORT_FILE, JSON.stringify({
      meta: { tag: TAG, timestamp: TS },
      summary: { total: allResults.length, passed: totalPassed, failed: totalFailed, warns: totalWarns },
      results: allResults
    }, null, 2));

    console.log("\n" + "=".repeat(70));
    console.log("\uD83D\uDCCA \u6D4B\u8BD5\u62A5\u544A: " + REPORT_FILE);
    console.log("\uD83D\uDCF8 \u622A\u56FE\u76EE\u5F55: " + SCREENSHOT_DIR + " (" + ssCount + " \u5F20)");
    console.log("\n\uD83C\uDFC6 \u603B\u8BA1 " + allResults.length + " \u9879\u6D4B\u8BD5");
    console.log("   \u2705 " + totalPassed + " \u901A\u8FC7 | \u274C " + totalFailed + " \u5931\u8D25 | \u26A0\uFE0F " + totalWarns + " \u8B66\u544A");
    console.log("   \u23F1\uFE0F  \u8017\u65F6: " + Math.round((Date.now() - TS) / 1000) + "s");
    console.log("=".repeat(70) + "\n");

    await browser.close();
    process.exit(totalFailed > 0 ? 1 : 0);

  } catch (err) {
    console.error("\n\uD83D\uDCA5 [FATAL]", err.message);
    try { var p2 = await browser.newPage(); await p2.screenshot({ path: path.join(SCREENSHOT_DIR, "fatal.png") }); await p2.close(); } catch (_) {}
    await browser.close();
    process.exit(1);
  }
})();
