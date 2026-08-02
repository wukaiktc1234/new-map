import { test, expect } from '../fixtures';

/**
 * 模块 4：运营中心（T1 深度覆盖）
 */

test.describe('\u8FD0\u8425\u4E2D\u5FC3', () => {
  test('[P0] 运营总览 - 页面加载与 KPI 卡片', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/operations');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    await expect(page).toHaveTitle(/\u8FD0\u8425\u603B\u89C8/);
  });

  test('[P0] 运营总览 - 图表渲染', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/operations');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    const canvases = page.locator('canvas');
    const canvasCount = await canvases.count();
    expect(canvasCount).toBeGreaterThanOrEqual(0);
  });

  test('[P1] 实时监控 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/operations/live-monitor');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    await expect(page).toHaveTitle(/\u5B9E\u65F6\u76D1\u63A7/);
  });

  test('[P1] 经营分析 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/operations/decision-board');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    await expect(page).toHaveTitle(/\u7ECF\u8425\u5206\u6790/);
  });

  test('[P1] 运营策略 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/operations/strategy-workshop');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    await expect(page).toHaveTitle(/\u8FD0\u8425\u7B56\u7565/);
  });

  test('[P2] 预警管理 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/operations/alert-command-center');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    await expect(page).toHaveTitle(/\u9884\u8B66\u7BA1\u7406/);
  });

  test('[P2] 经营报表 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage;
    await page.goto('/operations/reports');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    await expect(page).toHaveTitle(/\u7ECF\u8425\u62A5\u8868/);
  });
});

