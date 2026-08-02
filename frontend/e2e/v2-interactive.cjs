const { chromium } = require("playwright");
const path = require("path");
const fs = require("fs");

const RESULT_DIR = path.join(__dirname, "test-results", "v2-interactive");
const BASE = "http://localhost:3002";
const TIMESTAMP = Date.now();

if (!fs.existsSync(RESULT_DIR)) fs.mkdirSync(RESULT_DIR, { recursive: true });

let screenshotIndex = 0;
async function screenshot(page, label) {
  const idx = String(++screenshotIndex).padStart(2, "0");
  await page.screenshot({ path: path.join(RESULT_DIR, `${idx}-${label}.png`), fullPage: false });
  console.log(`  [OK] ${label}`);
}

async function waitForPage(page, url, timeout = 15000) {
  await page.goto(url, { waitUntil: "networkidle", timeout });
  await page.waitForTimeout(1500);
}

(async () => {
  console.log("=== 全量UI交互测试 v2 ===\n");
  const browser = await chromium.launch({ headless: true, channel: "chrome" });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();

  try {
    // ===== LOGIN =====
    console.log("[LOGIN]");
    await page.goto(`${BASE}/login`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1000);
    
    // Fill username and password
    const inputs = await page.locator("input").all();
    await inputs[0].fill("admin");
    await inputs[1].fill("Admin@123");
    
    // Click verify captcha button (button "验证")
    const btns = await page.locator("button").all();
    for (const btn of btns) {
      const text = await btn.textContent();
      if (text && text.includes("验证")) {
        await btn.click();
        break;
      }
    }
    await page.waitForTimeout(500);
    
    // Click login button
    for (const btn of btns) {
      const text = await btn.textContent();
      if (text && text.includes("登录")) {
        await btn.click();
        break;
      }
    }
    
    await page.waitForURL("**/home", { timeout: 15000 });
    await page.waitForTimeout(2000);
    await screenshot(page, "login-home");

    // ===== Module 1: 工作台 =====
    console.log("[Module 01] 工作台");
    await waitForPage(page, `${BASE}/home`);
    await screenshot(page, "dashboard");
    
    // ===== Module 2: 产品中心 =====
    console.log("[Module 02] 产品中心");
    await waitForPage(page, `${BASE}/product/food`);
    await screenshot(page, "product-food");
    await waitForPage(page, `${BASE}/product/category`);
    await screenshot(page, "product-category");
    await waitForPage(page, `${BASE}/product/package`);
    await screenshot(page, "product-package");
    await waitForPage(page, `${BASE}/product/pricing`);
    await screenshot(page, "product-pricing");

    // ===== Module 3: 订单管理 =====
    console.log("[Module 03] 订单管理");
    await waitForPage(page, `${BASE}/order/query`);
    await screenshot(page, "order-query");
    await waitForPage(page, `${BASE}/order/stats`);
    await screenshot(page, "order-stats");
    await waitForPage(page, `${BASE}/order/refund`);
    await screenshot(page, "order-refund");
    await waitForPage(page, `${BASE}/order/reservation`);
    await screenshot(page, "order-reservation");

    // ===== Module 4: 运营中心 =====
    console.log("[Module 04] 运营中心");
    await waitForPage(page, `${BASE}/operations/overview`);
    await screenshot(page, "ops-overview");
    await waitForPage(page, `${BASE}/operations/monitor`);
    await screenshot(page, "ops-monitor");
    await waitForPage(page, `${BASE}/operations/analysis`);
    await screenshot(page, "ops-analysis");
    await waitForPage(page, `${BASE}/operations/strategy`);
    await screenshot(page, "ops-strategy");

    // ===== Module 5: 门店管理 =====
    console.log("[Module 05] 门店管理");
    await waitForPage(page, `${BASE}/store/archive`);
    await screenshot(page, "store-archive");

    // ===== Module 6: 采购管理 =====
    console.log("[Module 06] 采购管理");
    await waitForPage(page, `${BASE}/purchase/supplier`);
    await screenshot(page, "purchase-supplier");
    await waitForPage(page, `${BASE}/purchase/order`);
    await screenshot(page, "purchase-order");

    // ===== Module 7: 仓储管理 =====
    console.log("[Module 07] 仓储管理");
    await waitForPage(page, `${BASE}/warehouse/inventory`);
    await screenshot(page, "warehouse-inventory");
    await waitForPage(page, `${BASE}/warehouse/overview`);
    await screenshot(page, "warehouse-overview");

    // ===== Module 8: 会员管理 =====
    console.log("[Module 08] 会员管理");
    await waitForPage(page, `${BASE}/member/list`);
    await screenshot(page, "member-list");
    await waitForPage(page, `${BASE}/member/level`);
    await screenshot(page, "member-level");

    // ===== Module 9: 财务中心 =====
    console.log("[Module 09] 财务中心");
    await waitForPage(page, `${BASE}/finance/ledger`);
    await screenshot(page, "finance-ledger");
    await waitForPage(page, `${BASE}/finance/payable`);
    await screenshot(page, "finance-payable");
    await waitForPage(page, `${BASE}/finance/expense`);
    await screenshot(page, "finance-expense");

    // ===== Module 10: 资产管理 =====
    console.log("[Module 10] 资产管理");
    await waitForPage(page, `${BASE}/asset/list`);
    await screenshot(page, "asset-list");

    // ===== Module 11: 人事管理 =====
    console.log("[Module 11] 人事管理");
    await waitForPage(page, `${BASE}/hr/employee`);
    await screenshot(page, "hr-employee");
    await waitForPage(page, `${BASE}/hr/contract`);
    await screenshot(page, "hr-contract");
    await waitForPage(page, `${BASE}/hr/salary`);
    await screenshot(page, "hr-salary");
    await waitForPage(page, `${BASE}/hr/attendance`);
    await screenshot(page, "hr-attendance");

    // ===== Module 12: 溯源管理 =====
    console.log("[Module 12] 溯源管理");
    await waitForPage(page, `${BASE}/traceability/overview`);
    await screenshot(page, "trace-overview");
    await waitForPage(page, `${BASE}/traceability/list`);
    await screenshot(page, "trace-list");

    // ===== Module 13: 设备管理 =====
    console.log("[Module 13] 设备管理");
    await waitForPage(page, `${BASE}/device/list`);
    await screenshot(page, "device-list");
    await waitForPage(page, `${BASE}/device/monitor`);
    await screenshot(page, "device-monitor");

    // ===== Module 14: 电子签章 =====
    console.log("[Module 14] 电子签章");
    await waitForPage(page, `${BASE}/seal/manage`);
    await screenshot(page, "seal-manage");

    // ===== Module 15: 系统管理 =====
    console.log("[Module 15] 系统管理");
    await waitForPage(page, `${BASE}/system/config`);
    await screenshot(page, "system-config");
    await waitForPage(page, `${BASE}/system/permission`);
    await screenshot(page, "system-permission");
    await waitForPage(page, `${BASE}/system/audit`);
    await screenshot(page, "system-audit");

    // ===== Module 16: 个人中心 =====
    console.log("[Module 16] 个人中心");
    await waitForPage(page, `${BASE}/personal/profile`);
    await screenshot(page, "personal-profile");

    console.log(`\n=== 完成! ${screenshotIndex} 张截图 ===`);
  } catch (err) {
    console.error(`[ERROR] ${err.message}`);
    await page.screenshot({ path: path.join(RESULT_DIR, "error.png") });
  } finally {
    await browser.close();
  }
})();
