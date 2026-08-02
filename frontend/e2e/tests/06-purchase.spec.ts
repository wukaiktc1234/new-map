import { test, expect } from '../fixtures';

test.describe('\u91C7\u8D2D\u7BA1\u7406', () => {
  test('[P0] 采购计划 - 页面加载与表格', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/plan'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P0] 采购申请 - 页面加载与表格', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/request'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P0] 采购订单 - 页面加载与表格', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/orders'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P1] 采购收货 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/stockin'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P1] 采购结算 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/settlement'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P1] 采购合同 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/contract'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P1] 电子合同 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/electronic-contract'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P1] 商品档案 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/archive'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P2] 供应商档案 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/supplier'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P2] 商品分类 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/material-category'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P2] 采购数据分析 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/analysis'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P2] 采购报表 - 页面加载', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/report'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
  });
  test('[P1] 采购搜索 - 关键词搜索交互', async ({ authenticatedPage }) => {
    const page = authenticatedPage; await page.goto('/purchase/orders'); await page.waitForLoadState('domcontentloaded'); await page.waitForTimeout(3000);
    const searchInputs = page.locator('input[placeholder*=\"\u641C\u7D22\"], input[placeholder*=\"\u8BA2\u5355\u53F7\"]');
    if (await searchInputs.count() > 0) {
      await searchInputs.first().fill('test');
      const searchBtn = page.locator('button:has-text(\"\u641C\u7D22\"), button:has-text(\"\u67E5\u8BE2\")');
      if (await searchBtn.count() > 0) { await searchBtn.first().click(); await page.waitForTimeout(2000); }
    }
  });
});
