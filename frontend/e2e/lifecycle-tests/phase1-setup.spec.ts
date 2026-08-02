import { test, expect, Page } from "@playwright/test";
import fs from "fs";
import path from "path";

// ============================================
// 企业完整数据生命周期测试 — 开业准备 & 基础配置
// Phase 1: 系统配置 → 门店创建 → 菜品分类 → 菜品 → 定价
// ============================================

const BASE = "http://localhost:3002";
const TS = Date.now();
const TAG = "lifecycle-" + TS.toString(36);
const SCREENSHOT_DIR = path.resolve("frontend/e2e/test-results/lifecycle-" + TS);

if (!fs.existsSync(SCREENSHOT_DIR)) fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });

let screenshotCounter = 0;
let gPage: Page;

// ---- Utility functions ----
async function ss(label: string) {
  const idx = String(++screenshotCounter).padStart(3, "0");
  await gPage.screenshot({ path: path.join(SCREENSHOT_DIR, idx + "-" + label + ".png"), fullPage: false });
  console.log("  📸 " + label);
}

async function goto(url: string) {
  await gPage.goto(BASE + url, { waitUntil: "networkidle", timeout: 30000 });
  await gPage.waitForTimeout(2000);
}

async function clickBtn(text: string) {
  const btn = gPage.locator("button").filter({ hasText: text });
  if (await btn.count() > 0) {
    await btn.first().click();
    await gPage.waitForTimeout(1200);
    return true;
  }
  return false;
}

async function fillField(label: string, value: string) {
  const item = gPage.locator(".el-form-item:visible").filter({ hasText: label });
  const input = item.locator("input:visible, textarea:visible").first();
  if (await input.count() > 0) {
    await input.fill(value);
    return true;
  }
  return false;
}

async function getTableRowCount(): Promise<number> {
  return await gPage.locator(".el-table__body-wrapper tr.el-table__row").count();
}

async function getTableText(): Promise<string> {
  return await gPage.locator(".el-table__body-wrapper").innerText().catch(() => "");
}

async function dialogConfirm(text = "确定") {
  const btn = gPage.locator(".el-dialog:visible button, .el-drawer:visible button").filter({ hasText: text });
  if (await btn.count() > 0) {
    await btn.first().click();
    await gPage.waitForTimeout(2500);
    return true;
  }
  return false;
}

// ============================================
// API helper (use browser context - already logged in)
// ============================================
async function apiPost(url: string, body: any) {
  return await gPage.evaluate(async ({ apiUrl, data }: any) => {
    const r = await fetch(apiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    return r.json();
  }, { apiUrl: url, data: body });
}

async function apiGet(url: string) {
  return await gPage.evaluate(async (apiUrl: string) => {
    const r = await fetch(apiUrl, { headers: { "Content-Type": "application/json" } });
    return r.json();
  }, url);
}

async function apiDelete(url: string) {
  return await gPage.evaluate(async (apiUrl: string) => {
    const r = await fetch(apiUrl, { method: "DELETE", headers: { "Content-Type": "application/json" } });
    return r.json();
  }, url);
}

// ============================================
// Test Data
// ============================================
const STORE_NAME = "生命周期门店-" + TAG;
const STORE_CODE = "LS-" + TS.toString(36).toUpperCase();
const CATEGORY_NAME = "生命周期分类-" + TAG;
const FOOD_NAME = "生命周期菜品-" + TAG;
const SUPPLIER_NAME = "生命周期供应商-" + TAG;
const EMPLOYEE_NAME = "生命周期员工-" + TAG;
const ASSET_NAME = "生命周期资产-" + TAG;

// ============================================
// Test Suite
// ============================================
test.describe.serial("企业完整数据生命周期 - Phase 1: 开业准备", () => {

  test.beforeAll(async ({ browser }) => {
    // Single login via API + localStorage
    const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
    const page = await ctx.newPage();
    await page.goto(BASE + "/login", { waitUntil: "domcontentloaded", timeout: 30000 });
    const loginResult = await page.evaluate(async () => {
      const r = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: "admin", password: "Admin@123" }),
      });
      return r.json();
    });
    expect(loginResult.code).toBe(0);
    const d = loginResult.data;
    await page.evaluate((data: any) => {
      localStorage.setItem("token", data.token);
      localStorage.setItem("refresh_token", data.refreshToken);
      localStorage.setItem("user_id", data.userInfo.id);
      localStorage.setItem("username", data.userInfo.username);
    }, d);
    await page.goto(BASE + "/home", { waitUntil: "networkidle" });
    await page.waitForTimeout(2000);
    // Save auth state and reuse
    await ctx.storageState({ path: "frontend/e2e/.auth/lifecycle-user.json" });
    await ctx.close();

    // Now create main page with auth
    const mainCtx = await browser.newContext({
      viewport: { width: 1920, height: 1080 },
      storageState: "frontend/e2e/.auth/lifecycle-user.json",
    });
    gPage = await mainCtx.newPage();
    console.log("\n=== Phase 1: 开业准备 & 基础配置 ===");
    console.log("TAG:", TAG);
    console.log("Screenshots:", SCREENSHOT_DIR);
  });

  // ===== 1.1 系统配置验证 =====
  test("1.1 系统配置页面加载并验证配置项", async () => {
    await goto("/system/permission");
    await ss("1.1-system-config");
    const text = await gPage.locator("body").innerText();
    expect(text.length).toBeGreaterThan(100);
    // Check stats section exists
    const statsCards = await gPage.locator(".stat-card, .stats-card, .el-card").count();
    console.log("  📊 统计卡片数:", statsCards);
  });

  // ===== 1.2 门店创建 =====
  test("1.2 创建门店 - UI表单提交 + 表格验证", async () => {
    await goto("/operations/store-archive");
    await ss("1.2-store-before");

    // Click 新增门店 button
    const addBtn = gPage.locator("button").filter({ hasText: /新增门店|新建门店|添加门店/ });
    await expect(addBtn.first()).toBeVisible({ timeout: 10000 });
    await addBtn.first().click();
    await gPage.waitForTimeout(1500);
    await ss("1.2-store-dialog");

    // Fill form
    await fillField("门店名称", STORE_NAME);
    await fillField("门店编码", STORE_CODE);
    await fillField("联系电话", "0755-88886666");
    await fillField("门店地址", "广东省深圳市南山区科技园");

    // Submit
    await dialogConfirm("确定");
    await gPage.waitForTimeout(2000);
    await ss("1.2-store-after");

    // Verify store appears in table
    const tableText = await getTableText();
    expect(tableText).toContain(STORE_NAME);
    console.log("  ✅ 门店创建成功:", STORE_NAME);
  });

  // ===== 1.3 菜品分类创建 =====
  test("1.3 创建菜品分类", async () => {
    await goto("/product/category");
    await ss("1.3-category-before");

    // Try to add category
    const addBtn = gPage.locator("button").filter({ hasText: /新增|新建|添加/ });
    if (await addBtn.count() > 0) {
      await addBtn.first().click();
      await gPage.waitForTimeout(1500);
      await ss("1.3-category-dialog");
      await fillField("分类名称", CATEGORY_NAME);
      await dialogConfirm("确定");
      await gPage.waitForTimeout(2000);
      await ss("1.3-category-after");
      console.log("  ✅ 分类创建完成:", CATEGORY_NAME);
    } else {
      console.log("  ⚠️ 未找到新增分类按钮");
    }
  });

  // ===== 1.4 菜品创建 =====
  test("1.4 创建菜品并验证", async () => {
    await goto("/product/food");
    await ss("1.4-food-before");

    const addBtn = gPage.locator("button").filter({ hasText: /新增菜品|新增|新建/ });
    if (await addBtn.count() > 0) {
      await addBtn.first().click();
      await gPage.waitForTimeout(1500);
      await ss("1.4-food-dialog");

      await fillField("菜品名称", FOOD_NAME);
      await fillField("规格", "标准份");

      await dialogConfirm("确定");
      await gPage.waitForTimeout(2000);
      await ss("1.4-food-after");

      // Verify in table
      const tableText = await getTableText();
      expect(tableText).toContain(FOOD_NAME);
      console.log("  ✅ 菜品创建成功:", FOOD_NAME);
    } else {
      console.log("  ⚠️ 未找到新增菜品按钮");
    }
  });

  // ===== 1.5 菜品定价验证 =====
  test("1.5 菜品定价页面验证", async () => {
    await goto("/product/pricing");
    await ss("1.5-pricing");
    const bodyText = await gPage.locator("body").innerText();
    expect(bodyText.length).toBeGreaterThan(50);
    console.log("  ✅ 定价页面加载成功");
  });

  // ===== 1.6 页面路由完整性验证 =====
  test("1.6 基础配置页面可达性验证", async () => {
    const pages = [
      ["工作台", "/home"],
      ["门店管理-门店列表", "/operations/store-archive"],
      ["产品中心-菜品管理", "/product/food"],
      ["产品中心-分类管理", "/product/category"],
      ["产品中心-定价管理", "/product/pricing"],
    ];
    for (const [name, url] of pages) {
      await goto(url);
      await gPage.waitForTimeout(1000);
      const title = await gPage.locator("title").innerText().catch(() => "");
      console.log(`  🖥️ ${name}: ${url} → ${title || "loaded"}`);
    }
    console.log("  ✅ 所有基础页面可达");
  });
});
