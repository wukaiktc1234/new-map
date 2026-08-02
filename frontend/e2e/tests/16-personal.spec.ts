import { test, expect } from '../fixtures';

/**
 * 模块 16：个人中心（T3 冒烟覆盖）
 */

test.describe('\u4E2A\u4EBA\u4E2D\u5FC3', () => {
  test('[P0] 个人中心 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/personal-center');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    await expect(page).toHaveTitle(/\u4E2A\u4EBA\u4E2D\u5FC3/);
  });

  test('[P1] 个人信息显示', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/personal-center');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // 验证页面有内容
    const bodyText = await page.locator('body').innerText();
    expect(bodyText.length).toBeGreaterThan(50);
  });
});

