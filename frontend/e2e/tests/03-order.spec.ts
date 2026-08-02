import { test, expect } from '../fixtures';

test.describe('\u8BA2\u5355\u7BA1\u7406', () => {
  test('[P0] 订单查询 - 页面加载与表格渲染', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/order/query');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u8BA2\u5355\u67E5\u8BE2/);
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10000 });
  });

  test('[P0] 订单查询 - 条件搜索', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/order/query');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    const searchInputs = page.locator('input[placeholder*=\"\u8BA2\u5355\u53F7\"], input[placeholder*=\"\u5173\u952E\"], input[placeholder*=\"\u641C\u7D22\"]');
    if (await searchInputs.count() > 0) {
      await searchInputs.first().fill('test');
      const searchBtn = page.locator('button:has-text(\"\u641C\u7D22\"), button:has-text(\"\u67E5\u8BE2\")');
      if (await searchBtn.count() > 0) { await searchBtn.first().click(); await page.waitForTimeout(3000); }
    }
  });

  test('[P1] 订单统计 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/order/statistics');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u8BA2\u5355\u7EDF\u8BA1/);
  });

  test('[P1] 退款管理 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/order/refund');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u9000\u6B3E\u7BA1\u7406/);
  });

  test('[P1] 预约管理 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/order/reservation');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u9884\u7EA6\u7BA1\u7406/);
  });
});

