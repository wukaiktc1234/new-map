const { chromium } = require("playwright");
const path = require("path");
const fs = require("fs");

const BASE = "http://localhost:3002";
const AUTH_FILE = path.resolve("frontend/e2e/.auth/deep-user.json");
const TS = Date.now();
const TAG = "e2e-deep-" + TS.toString(36);
const SCREENSHOT_DIR = path.resolve("frontend/e2e/test-results/deep-" + TS);

// Ensure directories exist
if (!fs.existsSync(path.dirname(AUTH_FILE))) fs.mkdirSync(path.dirname(AUTH_FILE), { recursive: true });
if (!fs.existsSync(SCREENSHOT_DIR)) fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });

let screenshotCounter = 0;
async function screenshot(page, label) {
  const idx = String(++screenshotCounter).padStart(3, "0");
  const p = path.join(SCREENSHOT_DIR, idx + "-" + label + ".png");
  await page.screenshot({ path: p, fullPage: false });
  console.log("  [SCREENSHOT] " + label);
}

async function loginOnce(browser) {
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  await page.goto(BASE + "/login", { waitUntil: "domcontentloaded", timeout: 30000 });
  const loginResult = await page.evaluate(async () => {
    const r = await fetch("/api/v1/auth/login", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: "admin", password: "Admin@123" }),
    });
    return r.json();
  });
  if (loginResult.code !== 0) {
    console.error("Login failed:", JSON.stringify(loginResult));
    throw new Error("Login failed: code=" + loginResult.code);
  }
  await page.evaluate((d) => {
    localStorage.setItem("token", d.token || d.data?.token);
    localStorage.setItem("refresh_token", d.refreshToken || d.data?.refreshToken);
    localStorage.setItem("user_id", d.userInfo?.id || d.data?.userInfo?.id || "");
    localStorage.setItem("username", d.userInfo?.username || "admin");
  }, loginResult.data || loginResult);
  await page.context().storageState({ path: AUTH_FILE });
  console.log("  [AUTH] Login OK, storage saved to", AUTH_FILE);
  await page.close();
}

async function gotoAndWait(page, url) {
  await page.goto(BASE + url, { waitUntil: "networkidle", timeout: 30000 });
  await page.waitForTimeout(2000);
}

async function clickBtn(page, btnText) {
  const btn = page.locator("button").filter({ hasText: btnText });
  if (await btn.count() > 0) {
    await btn.first().click();
    await page.waitForTimeout(1200);
    return true;
  }
  return false;
}

async function fillField(page, labelText, value) {
  const item = page.locator(".el-form-item:visible").filter({ hasText: labelText });
  const input = item.locator("input:visible, textarea:visible").first();
  if (await input.count() > 0) {
    await input.fill(value);
    return true;
  }
  return false;
}

async function selectOption(page, labelText, optionText) {
  const item = page.locator(".el-form-item:visible").filter({ hasText: labelText });
  const trigger = item.locator(".el-select .el-select__wrapper, .el-select .el-input__wrapper").first();
  if (await trigger.count() > 0) {
    await trigger.click();
    await page.waitForTimeout(600);
    const option = page.locator(".el-select-dropdown:visible .el-select-dropdown__item, .el-popper:visible .el-select-dropdown__item").filter({ hasText: optionText });
    if (await option.count() > 0) {
      await option.first().click();
      await page.waitForTimeout(400);
      return true;
    }
    // Click elsewhere to close dropdown
    await page.locator(".el-dialog:visible .el-dialog__header").first().click().catch(() => {});
    await page.waitForTimeout(300);
  }
  return false;
}

async function clickDialogConfirm(page, btnText) {
  const btn = page.locator(".el-dialog:visible button, .el-drawer:visible button").filter({ hasText: btnText });
  if (await btn.count() > 0) {
    await btn.first().click();
    await page.waitForTimeout(2500);
    return true;
  }
  return false;
}

(async () => {
  console.log("=== Enterprise Deep Interaction Test Suite ===\n");
  console.log("TAG:", TAG);
  console.log("Timestamp:", TS);
  
  const browser = await chromium.launch({ headless: true, channel: "chrome", args: ["--disable-blink-features=AutomationControlled"] });
  
  try {
    // Phase 0: Login once
    console.log("\n[Phase 0] Login");
    await loginOnce(browser);
    
    // Create a context with saved auth state
    const context = await browser.newContext({
      viewport: { width: 1920, height: 1080 },
      storageState: AUTH_FILE,
    });
    const page = await context.newPage();
    
    // ===== PHASE 1: Store Setup =====
    console.log("\n[Phase 1] Store & Product Setup");
    
    console.log("  1.1 Create Store via UI dialog");
    await gotoAndWait(page, "/operations/store-archive");
    await screenshot(page, "1.1-before");
    
    await clickBtn(page, "新增门店");
    await page.waitForTimeout(1000);
    await screenshot(page, "1.1-dialog");
    
    await fillField(page, "门店名称", "AutoStore-" + TAG);
    await fillField(page, "门店编码", "AS-" + TS.toString(36).toUpperCase());
    await selectOption(page, "门店类型", "直营");
    await fillField(page, "联系电话", "13800009999");
    await fillField(page, "面积", "200");
    
    await clickDialogConfirm(page, "确定");
    await page.waitForTimeout(2000);
    await screenshot(page, "1.1-after");
    
    const storeBody = await page.locator("body").innerText().catch(() => "");
    const storeFound = storeBody.includes("AutoStore-" + TAG);
    console.log("  Store in table:", storeFound);
    
    // Continue to product category
    console.log("  1.2 Create Product Category");
    await gotoAndWait(page, "/product/category");
    await screenshot(page, "1.2-category");
    
    await clickBtn(page, "新增");
    await page.waitForTimeout(1500);
    await screenshot(page, "1.2-category-dialog");
    
    const catInput = page.locator(".el-dialog:visible input, .el-drawer:visible input").first();
    if (await catInput.count() > 0) {
      await catInput.fill("DeepCategory-" + TAG);
    }
    await clickDialogConfirm(page, "确定");
    await page.waitForTimeout(2000);
    await screenshot(page, "1.2-category-result");
    
    // Create food item
    console.log("  1.3 Create Food Item");
    await gotoAndWait(page, "/product/food");
    await screenshot(page, "1.3-food-before");
    
    await clickBtn(page, "新增菜品");
    await page.waitForTimeout(1500);
    await screenshot(page, "1.3-food-dialog");
    
    await fillField(page, "菜品名称", "DeepFood-" + TAG);
    await fillField(page, "规格", "Standard");
    
    await clickDialogConfirm(page, "保存");
    await page.waitForTimeout(2000);
    await screenshot(page, "1.3-food-after");
    
    // ===== PHASE 2: Supplier & Warehouse =====
    console.log("\n[Phase 2] Procurement & Warehouse");
    
    console.log("  2.1 Create Supplier");
    await gotoAndWait(page, "/purchase/supplier");
    await screenshot(page, "2.1-supplier-before");
    
    await clickBtn(page, "新增供应商");
    await page.waitForTimeout(1500);
    await screenshot(page, "2.1-supplier-dialog");
    
    await fillField(page, "供应商名称", "DeepSupp-" + TAG);
    await fillField(page, "供应商编码", "S-" + TS.toString(36).toUpperCase());
    await fillField(page, "联系人", "Test Contact");
    await fillField(page, "联系电话", "13800009998");
    
    await clickDialogConfirm(page, "确定");
    await page.waitForTimeout(2000);
    await screenshot(page, "2.1-supplier-after");
    
    // Warehouse
    console.log("  2.2 Warehouse Inventory");
    await gotoAndWait(page, "/warehouse/inventory");
    await screenshot(page, "2.2-warehouse");
    
    const invRows = await page.locator(".el-table__body-wrapper tr.el-table__row").count();
    console.log("  Inventory rows:", invRows);
    
    // ===== PHASE 3: HR & Employee =====
    console.log("\n[Phase 3] HR & Finance");
    
    console.log("  3.1 Create Employee");
    await gotoAndWait(page, "/hr/employee");
    await screenshot(page, "3.1-employee-before");
    
    await clickBtn(page, "新增员工");
    await page.waitForTimeout(1500);
    await screenshot(page, "3.1-employee-dialog");
    
    const empName = "DeepEmp-" + TAG;
    await fillField(page, "姓名", empName);
    await fillField(page, "员工编号", "EMP-" + TS.toString(36).toUpperCase());
    await fillField(page, "手机号", "13900009999");
    
    await clickDialogConfirm(page, "确定");
    await page.waitForTimeout(2000);
    await screenshot(page, "3.1-employee-after");
    
    // Finance
    console.log("  3.2 Finance Ledger");
    await gotoAndWait(page, "/finance/ledger");
    await screenshot(page, "3.2-ledger");
    
    console.log("  3.3 Finance Expense");
    await gotoAndWait(page, "/finance/expense");
    await screenshot(page, "3.3-expense");
    
    // ===== PHASE 4: Operations & Orders & Members =====
    console.log("\n[Phase 4] Operations & Orders & Members");
    
    console.log("  4.1 Order Query");
    await gotoAndWait(page, "/order/query");
    await page.waitForTimeout(1000);
    await clickBtn(page, "查询");
    await page.waitForTimeout(2000);
    await screenshot(page, "4.1-orders");
    const orderRows = await page.locator(".el-table__body-wrapper tr.el-table__row").count();
    console.log("  Order rows:", orderRows);
    
    console.log("  4.2 Operations Analysis");
    await gotoAndWait(page, "/operations/analysis");
    await screenshot(page, "4.2-analysis");
    
    console.log("  4.3 Member List");
    await gotoAndWait(page, "/member/list");
    await screenshot(page, "4.3-members");
    
    // ===== PHASE 5: Asset & Device =====
    console.log("\n[Phase 5] Asset & Device");
    
    console.log("  5.1 Asset Ledger");
    await gotoAndWait(page, "/asset/ledger");
    await screenshot(page, "5.1-asset");
    const assetRows = await page.locator(".el-table__body-wrapper tr.el-table__row").count();
    console.log("  Asset rows:", assetRows);
    
    console.log("  5.2 Device List");
    await gotoAndWait(page, "/device/list");
    await screenshot(page, "5.2-device");
    
    // ===== PHASE 6: Compliance =====
    console.log("\n[Phase 6] Traceability, Seal & System");
    
    console.log("  6.1 Traceability");
    await gotoAndWait(page, "/traceability/list");
    await screenshot(page, "6.1-trace");
    
    console.log("  6.2 Seal Management");
    await gotoAndWait(page, "/seal/manage");
    await screenshot(page, "6.2-seal");
    
    console.log("  6.3 System Permission Center");
    await gotoAndWait(page, "/system/permission-center");
    await screenshot(page, "6.3-system");
    
    // ===== PHASE 7: Dashboard & Personal =====
    console.log("\n[Phase 7] Dashboard & Personal");
    
    console.log("  7.1 Home Dashboard");
    await gotoAndWait(page, "/home");
    await screenshot(page, "7.1-home");
    
    console.log("  7.2 Personal Profile");
    await gotoAndWait(page, "/personal/profile");
    await screenshot(page, "7.2-profile");
    
    await browser.close();
    console.log("\n=== DONE! " + screenshotCounter + " screenshots taken ===");
    
  } catch (err) {
    console.error("\n[FATAL ERROR]", err.message);
    try {
      const page = await browser.newPage();
      await page.screenshot({ path: path.join(SCREENSHOT_DIR, "fatal-error.png") });
      await page.close();
    } catch(e) { /* ignore */ }
    await browser.close();
    process.exit(1);
  }
})();
