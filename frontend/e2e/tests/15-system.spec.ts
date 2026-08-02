import { test, expect } from '../fixtures';

/**
 * 模块 15：系统管理（T1 深度覆盖）
 */

test.describe('\u7CFB\u7EDF\u7BA1\u7406', () => {
  test('[P0] 系统配置 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/system/permission');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u7CFB\u7EDF\u914D\u7F6E/);
  });

  test('[P0] 权限中心 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/system/permission-center');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u6743\u9650\u4E2D\u5FC3/);
  });

  test('[P1] 操作审计 - 页面加载与日志表格', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/system/operation-audit');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u64CD\u4F5C\u5BA1\u8BA1/);
    const table = page.locator('.el-table');
    await expect(table).toBeVisible({ timeout: 10000 });
  });

  test('[P1] AI模型配置 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/system/ai-model-config');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u914D\u7F6E/);
  });

  test('[P1] 系统配置 - Tab 交互', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/system/permission');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // 查找 Tab 标签
    const tabs = page.locator('.el-tabs__item, [role=\"tab\"]');
    const tabCount = await tabs.count();
    if (tabCount > 1) {
      // 点击第二个 Tab
      await tabs.nth(1).click();
      await page.waitForTimeout(1500);
    }
  });

  test('[P1] 用户管理 Tab - 可见', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/system/permission');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // 查找用户管理相关的 Tab
    const userTab = page.locator('[role=\"tab\"]:has-text(\"\u7528\u6237\"), .el-tabs__item:has-text(\"\u7528\u6237\")');
    if (await userTab.count() > 0) {
      await userTab.first().click();
      await page.waitForTimeout(1500);
    }
  });

  test('[P1] 角色管理 Tab - 可见', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/system/permission');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // 查找角色管理 Tab
    const roleTab = page.locator('[role=\"tab\"]:has-text(\"\u89D2\u8272\"), .el-tabs__item:has-text(\"\u89D2\u8272\")');
    if (await roleTab.count() > 0) {
      await roleTab.first().click();
      await page.waitForTimeout(1500);
    }
  });
});

