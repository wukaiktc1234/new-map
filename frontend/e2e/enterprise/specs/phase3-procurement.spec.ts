import { test, expect } from "@playwright/test";
const BASE = "http://localhost:3002";
const TS = Date.now();

async function login(page) {
  await page.goto(BASE + "/login", { waitUntil: "networkidle" });
  await page.waitForTimeout(1500);
  const inputs = page.locator("input");
  const cnt = await inputs.count();
  if (cnt >= 2) { await inputs.nth(0).fill("admin"); await inputs.nth(1).fill("Admin@123"); }
  const lb = page.locator("button").filter({ hasText: /login/i }).first();
  if (await lb.isVisible({ timeout: 3000 }).catch(() => false)) { await lb.click(); await page.waitForTimeout(3000); }
  await page.waitForURL(BASE + "/**", { timeout: 15000 }).catch(() => {});
}

const PAGES = [
  ["3.0 Supplier","/purchase/supplier"],
  ["3.01 Orders","/purchase/orders"],
  ["3.02 Archive","/purchase/archive"],
  ["3.03 Category","/purchase/material-category"],
  ["3.04 Plan","/purchase/plan"],
  ["3.05 StockIn","/purchase/stockin"],
  ["3.06 Contract","/purchase/contract"],
  ["3.07 Settlement","/purchase/settlement"],
  ["3.08 MaterialReq","/purchase/material-request"],
  ["3.09 Report","/purchase/report"],
  ["3.10 Analysis","/purchase/analysis"],
  ["3.11 Overview","/warehouse/overview"],
  ["3.12 StoreInv","/warehouse/store-inventory"],
  ["3.13 Inventory","/warehouse/inventory"],
  ["3.14 Outbound","/warehouse/outbound"],
  ["3.15 Adjust","/warehouse/adjust"],
  ["3.16 Loss","/warehouse/inventory-loss"],
  ["3.17 Warning","/warehouse/warning"],
  ["3.18 Check","/warehouse/check"],
  ["3.19 Report","/warehouse/report"],
  ["3.20 Location","/warehouse/location"],
  ["3.21 Transfer","/warehouse/transfer"],
];

test.describe("Phase 3 Procurement and Warehouse", () => {
  test.beforeEach(async ({ page }) => { await login(page); });

  test("3.0 Supplier", async ({ page }) => {
    await page.goto(BASE+"/purchase/supplier",{waitUntil:"networkidle"});
    await page.waitForTimeout(3000);
    await expect(page.locator(".el-table")).toBeVisible({timeout:10000});
    await page.screenshot({path:"e2e/test-results/p3-supplier-"+TS+".png"});
  });

  test("3.01 Purchase Orders", async ({ page }) => {
    await page.goto(BASE+"/purchase/orders",{waitUntil:"networkidle"});
    await page.waitForTimeout(3000);
    await expect(page.locator(".el-table")).toBeVisible({timeout:10000});
    await page.screenshot({path:"e2e/test-results/p3-orders-"+TS+".png"});
  });

  for (const [name,path] of PAGES) {
    if (name==="3.0 Supplier"||name==="3.01 Orders") continue;
    test(name, async ({ page }) => {
      await page.goto(BASE+path,{waitUntil:"domcontentloaded"});
      await page.waitForTimeout(3000);
      const body = await page.locator("body").innerText().catch(()=>"");
      expect(body.length).toBeGreaterThan(0);
      expect(body.includes("404")||body.includes("Cannot GET")).toBe(false);
      await page.screenshot({path:"e2e/test-results/p3-"+name.replace(/\s+/g,"-")+"-"+TS+".png"});
    });
  }
});
