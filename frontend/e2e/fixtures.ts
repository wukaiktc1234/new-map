import { test as base, expect, Page } from '@playwright/test';

/**
 * 自定义 Fixtures
 * 提供已登录的 page 实例 + 辅助方法
 */

// 扩展的 page 对象
export type AuthenticatedPage = Page;

export const test = base.extend<{ authenticatedPage: AuthenticatedPage }>({
  authenticatedPage: async ({ page }, use) => {
    // 登录态由 storageState 确保
    await page.goto('/home');
    await page.waitForLoadState('domcontentloaded');
    await use(page);
  },
});

export { expect };

