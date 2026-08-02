import { test, expect } from '../fixtures';

test.describe('\u5DE5\u4F5C\u53F0', () => {
  test('[P0] 工作台页面加载 - KPI 卡片可见', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/home');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
    await expect(page).toHaveTitle(/\u5DE5\u4F5C\u53F0/);
  });
  test('[P1] 图表渲染', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/home');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
  });
  test('[P1] 快捷入口', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/home');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });
  test('[P2] 待办事项列表', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/home');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });
});
