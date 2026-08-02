import { test, expect } from '../fixtures';

test.describe('\u4EA7\u54C1\u4E2D\u5FC3', () => {
  test('[P0] 菜品管理 - 页面加载与表格渲染', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/product/food');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u83DC\u54C1\u7BA1\u7406/);
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10000 });
  });

  test('[P0] 菜品管理 - 搜索功能', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/product/food');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    const searchInputs = page.locator('input[placeholder*=\"\u641C\u7D22\"], input[placeholder*=\"\u5173\u952E\"], input[placeholder*=\"\u83DC\u54C1\u540D\"]');
    if (await searchInputs.count() > 0) {
      await searchInputs.first().fill('\u6D4B\u8BD5');
      const searchBtn = page.locator('button:has-text(\"\u641C\u7D22\"), button:has-text(\"\u67E5\u8BE2\")');
      if (await searchBtn.count() > 0) {
        await searchBtn.first().click();
        await page.waitForTimeout(3000);
      }
    }
  });

  test('[P1] 菜品分类 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/product/category');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u83DC\u54C1\u5206\u7C7B/);
  });

  test('[P1] 套餐管理 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/product/combo');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u5957\u9910\u7BA1\u7406/);
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 10000 });
  });

  test('[P1] 菜品定价 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/product/pricing');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u83DC\u54C1\u5B9A\u4EF7/);
  });

  test('[P2] 菜品成本分析 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/product/cost-analysis');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u83DC\u54C1\u6210\u672C\u5206\u6790/);
  });
});

