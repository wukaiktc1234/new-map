import { test, expect } from "@playwright/test";

test.describe("深度链接修复验证", () => {
  test("PermissionCenter 侧边栏 用户管理 → 切换 Tab", async ({ page }) => {
    // 登录
    await page.goto("/login");
    await page.waitForLoadState("domcontentloaded");
    await page.waitForTimeout(1000);

    const loginResult = await page.evaluate(async () => {
      const r = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: "admin", password: "Admin@123" }),
      });
      return r.json();
    });

    await page.evaluate((d: any) => {
      localStorage.setItem("token", d.data.token);
      localStorage.setItem("refresh_token", d.data.refreshToken);
      localStorage.setItem("user_id", d.data.userInfo.id);
      localStorage.setItem("username", d.data.userInfo.username);
    }, loginResult);

    // Go to permission center
    await page.goto("/system/permission-center");
    await page.waitForLoadState("domcontentloaded");
    await page.waitForTimeout(3000);

    // Verify the page loaded - check for permission-tabs
    const tabs = page.locator(".tab-item");
    await expect(tabs.first()).toBeVisible({ timeout: 10000 });

    // Get current active tab
    const activeBefore = await page.locator(".tab-item--active").first().textContent();

    // Click "用户管理" in sidebar menu
    const sidebarUser = page.locator('.el-menu-item:has-text("用户管理")').first();
    await expect(sidebarUser).toBeVisible({ timeout: 5000 });
    await sidebarUser.click();
    await page.waitForTimeout(2000);

    // Verify tab switched to 用户管理
    const activeAfter = await page.locator(".tab-item--active").first().textContent();
    expect(activeAfter?.trim()).toContain("用户管理");
  });

  test("SystemSettings 侧边栏 字典管理 → 切换 Tab", async ({ page }) => {
    // 登录
    await page.goto("/login");
    await page.waitForLoadState("domcontentloaded");
    await page.waitForTimeout(1000);

    const loginResult = await page.evaluate(async () => {
      const r = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: "admin", password: "Admin@123" }),
      });
      return r.json();
    });

    await page.evaluate((d: any) => {
      localStorage.setItem("token", d.data.token);
      localStorage.setItem("refresh_token", d.data.refreshToken);
      localStorage.setItem("user_id", d.data.userInfo.id);
      localStorage.setItem("username", d.data.userInfo.username);
    }, loginResult);

    // Go to system settings
    await page.goto("/system/permission");
    await page.waitForLoadState("domcontentloaded");
    await page.waitForTimeout(3000);

    // Verify the page loaded
    const tabs = page.locator(".tab-item");
    await expect(tabs.first()).toBeVisible({ timeout: 10000 });

    // Click "字典管理" in sidebar menu
    const sidebarDict = page.locator('.el-menu-item:has-text("字典管理")').first();
    await expect(sidebarDict).toBeVisible({ timeout: 5000 });
    await sidebarDict.click();
    await page.waitForTimeout(2000);

    // Verify tab switched to 字典管理
    const activeAfter = await page.locator(".tab-item--active").textContent();
    expect(activeAfter?.trim()).toContain("字典管理");
  });
});
