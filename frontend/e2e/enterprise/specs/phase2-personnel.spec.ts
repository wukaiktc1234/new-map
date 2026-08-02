import { test, expect } from "@playwright/test";
const BASE = "http://localhost:3002";
const TS = Date.now();
const TAG = "e2e-" + TS;

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

test.describe("Phase 2: Personnel - UI Operations", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("2.1 Employee page renders and stat cards visible", async ({ page }) => {
    await page.goto(BASE + "/hr/employee", { waitUntil: "networkidle" });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: "e2e/test-results/p2-2.1-employee-" + TS + ".png" });

    // Verify stat cards show (at least 3 cards)
    const statCards = page.locator(".stat-card, .el-statistic, .stat-item");
    const count = await statCards.count();
    console.log("Stat cards found:", count);
    expect(count).toBeGreaterThanOrEqual(3);

    // Verify table renders
    const table = page.locator(".el-table");
    await expect(table).toBeVisible({ timeout: 5000 });
  });

  test("2.2 Click add employee button, dialog opens", async ({ page }) => {
    await page.goto(BASE + "/hr/employee", { waitUntil: "networkidle" });
    await page.waitForTimeout(3000);

    // Find and click "新增员工" button
    const addBtn = page.getByRole("button", { name: /新增|添加员工|add/i }).or(
      page.locator("button").filter({ hasText: /新增/ })
    ).first();
    await expect(addBtn).toBeVisible({ timeout: 10000 });
    await addBtn.click();
    await page.waitForTimeout(1500);

    // Dialog should appear
    const dialog = page.getByRole("dialog").or(page.locator(".el-dialog")).first();
    await expect(dialog).toBeVisible({ timeout: 5000 });

    await page.screenshot({ path: "e2e/test-results/p2-2.2-dialog-" + TS + ".png" });
    console.log("Dialog opened successfully");
  });

  test("2.3 Navigate to all HR sub-pages", async ({ page }) => {
    const hrPages = [
      ["Contract", "/hr/contract"],
      ["Salary", "/hr/salary"],
      ["Attendance", "/hr/attendance"],
      ["Onboarding", "/hr/onboarding"],
      ["Training", "/hr/training"],
      ["Recruitment", "/hr/recruitment"],
      ["Organization", "/hr/organization"],
      ["Position", "/hr/position"],
      ["JobLevel", "/hr/job-level"],
    ];
    for (const [name, path] of hrPages) {
      await page.goto(BASE + path, { waitUntil: "domcontentloaded" });
      await page.waitForTimeout(2000);
      const body = await page.locator("body").innerText().catch(() => "");
      const isError = body.includes("404") || body.includes("Cannot GET");
      expect(isError).toBe(false);
      console.log(name + ": OK (" + body.substring(0, 30).replace(/\n/g, " ") + "...)");
    }
  });
});
