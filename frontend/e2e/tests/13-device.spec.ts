import { test, expect } from '../fixtures';

/**
 * 模块 13：设备管理（T2 标准覆盖）
 */

test.describe('\u8BBE\u5907\u7BA1\u7406', () => {
  test('[P0] 设备列表 - 页面加载与表格', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/device/list');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u8BBE\u5907\u5217\u8868/);
  });

  test('[P1] 设备监控 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/device/monitor');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u8BBE\u5907\u76D1\u63A7/);
  });

  test('[P1] 设备告警 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/device/alerts');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u8BBE\u5907\u544A\u8B66/);
  });

  test('[P2] 状态历史 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/device/status-history');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u72B6\u6001\u5386\u53F2/);
  });
});

