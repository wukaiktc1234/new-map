import { test, expect } from "@playwright/test";

const BASE = "http://localhost:3002";
const TS = Date.now();

test.describe("Phase 1: Enterprise Pre-opening Setup", () => {

  test("1.1 System Config page renders", async ({ page }) => {
    await page.goto(BASE + "/system/permission", { waitUntil: "networkidle" });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: "e2e/test-results/phase1-system-" + TS + ".png" });
    const bodyText = await page.locator("body").innerText().catch(() => "");
    expect(bodyText.length).toBeGreaterThan(0);
  });

  test("1.2 Store Archive page loads", async ({ page }) => {
    await page.goto(BASE + "/operations/store-archive", { waitUntil: "networkidle" });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: "e2e/test-results/phase1-store-" + TS + ".png" });
    const bodyText = await page.locator("body").innerText().catch(() => "");
    expect(bodyText.length).toBeGreaterThan(0);
  });

  test("1.3 Product Category page accessible", async ({ page }) => {
    await page.goto(BASE + "/product/category", { waitUntil: "networkidle" });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: "e2e/test-results/phase1-category-" + TS + ".png" });
  });

  test("1.4 Food management page", async ({ page }) => {
    await page.goto(BASE + "/product/food", { waitUntil: "networkidle" });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: "e2e/test-results/phase1-food-" + TS + ".png" });
  });

  test("1.5 Pricing page renders", async ({ page }) => {
    await page.goto(BASE + "/product/pricing", { waitUntil: "networkidle" });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: "e2e/test-results/phase1-pricing-" + TS + ".png" });
  });
});
