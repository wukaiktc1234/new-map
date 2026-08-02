const { chromium } = require("playwright");
const path = require("path");
const fs = require("fs");

const RESULT_DIR = path.join(__dirname, "..", "test-results", "interactive");
if (!fs.existsSync(RESULT_DIR)) fs.mkdirSync(RESULT_DIR, { recursive: true });

const BASE = "http://localhost:3002";
const TIMESTAMP = Date.now();
const TEST_PREFIX = `e2e-${TIMESTAMP}`;

async function screenshot(page, name) {
  await page.screenshot({ path: path.join(RESULT_DIR, `${name}.png`), fullPage: false });
  console.log(`[SCREENSHOT] ${name}.png`);
}

async function login(page) {
  await page.goto(`${BASE}/login`, { waitUntil: "networkidle" });
  await page.waitForTimeout(1000);
  // Fill login form
  const inputs = await page.locator("input[type='text'], input:not([type])").all();
  if (inputs.length >= 2) {
    await inputs[0].fill("admin");
    await inputs[1].fill("Admin@123");
  }
  // Click login button
  await page.locator("button[type='submit'], button:has-text('登录'), .login-btn").first().click();
  await page.waitForURL("**/home", { timeout: 15000 });
  await page.waitForTimeout(2000);
  console.log("[LOGIN] 登录成功");
  await screenshot(page, "00-home-dashboard");
}

async function run() {
  console.log("=== 企业生产模拟 - 全链路交互验证 ===");
  const browser = await chromium.launch({ headless: true, channel: "chrome" });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();

  try {
    await login(page);

    // ===== Phase 1: System Setup =====
    console.log("\n=== Phase 1: 开业准备 - 系统配置 ===");
    
    // 1.1 System Config
    await page.goto(`${BASE}/system/config`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "1.1-system-config");
    // Check config form elements
    const configForm = await page.locator("form, .el-form, .ant-form, .config-form").count();
    console.log(`[CHECK] 系统配置表单存在: ${configForm > 0}`);

    // 1.2 Product Category
    await page.goto(`${BASE}/product/category`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "1.2-category");

    // 1.3 Food Management - Create a product
    await page.goto(`${BASE}/product/food`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "1.3-food-list");
    
    // Try to click add button
    const addBtn = page.locator("button:has-text('新增'), button:has-text('添加'), .el-button--primary").first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);
      await screenshot(page, "1.3-food-add-dialog");
      // Fill form
      const dialogInputs = await page.locator(".el-dialog input, .ant-modal input, dialog input, [role='dialog'] input").all();
      if (dialogInputs.length > 0) {
        await dialogInputs[0].fill(`${TEST_PREFIX}-test-dish`);
        console.log("[ACTION] 填写菜品名称");
      }
      await screenshot(page, "1.3-food-form-filled");
    }

    // 1.4 Store Archive
    await page.goto(`${BASE}/store/archive`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "1.4-store-archive");

    // ===== Phase 2: Personnel =====
    console.log("\n=== Phase 2: 人员配置 ===");
    await page.goto(`${BASE}/hr/employee`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "2.1-employee");
    
    // Try to add employee
    const empAddBtn = page.locator("button:has-text('新增'), button:has-text('添加')").first();
    if (await empAddBtn.isVisible()) {
      await empAddBtn.click();
      await page.waitForTimeout(1000);
      await screenshot(page, "2.1-employee-add-dialog");
    }

    // ===== Phase 3: Procurement =====
    console.log("\n=== Phase 3: 采购管理 ===");
    await page.goto(`${BASE}/purchase/supplier`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "3.1-supplier");

    await page.goto(`${BASE}/purchase/order`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "3.2-purchase-order");

    // ===== Phase 4: Warehouse =====
    console.log("\n=== Phase 4: 仓储管理 ===");
    await page.goto(`${BASE}/warehouse/overview`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "4.1-warehouse-overview");

    await page.goto(`${BASE}/warehouse/inventory`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "4.2-inventory");

    // ===== Phase 5: Operations =====
    console.log("\n=== Phase 5: 运营中心 ===");
    await page.goto(`${BASE}/operations/overview`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "5.1-operations-overview");

    // ===== Phase 6: Orders =====
    console.log("\n=== Phase 6: 订单管理 ===");
    await page.goto(`${BASE}/order/query`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "6.1-order-query");

    // ===== Phase 7: Finance =====
    console.log("\n=== Phase 7: 财务中心 ===");
    await page.goto(`${BASE}/finance/ledger`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "7.1-ledger");

    // ===== Phase 8: System =====
    console.log("\n=== Phase 8: 系统管理 ===");
    await page.goto(`${BASE}/system/permission`, { waitUntil: "networkidle" });
    await page.waitForTimeout(1500);
    await screenshot(page, "8.1-permission");

    console.log("\n=== 交互验证完成! ===");
    const files = fs.readdirSync(RESULT_DIR);
    console.log(`共生成 ${files.length} 张截图`);

  } catch (err) {
    console.error(`[ERROR] ${err.message}`);
    await screenshot(page, "error-state");
  } finally {
    await browser.close();
  }
}

run().catch(console.error);
