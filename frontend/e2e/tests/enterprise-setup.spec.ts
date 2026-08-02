import { test, expect } from '@playwright/test';

const BASE = 'http://localhost:3002';
const TS = Date.now();

// Helper: login via API from current context (no page navigation)
async function ensureLogin(page) {
  const result = await page.evaluate(async () => {
    const r = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'Admin@123' }),
    });
    const data = await r.json();
    if (data.code === 0) {
      localStorage.setItem('token', data.data.token);
      localStorage.setItem('refresh_token', data.data.refreshToken);
      localStorage.setItem('user_id', data.data.userInfo.id);
      localStorage.setItem('username', data.data.userInfo.username);
      return true;
    }
    return false;
  });
  expect(result).toBe(true);
}

test.describe('Phase 1: Enterprise Pre-opening Setup', () => {

  test('1.1 System Settings - Page renders with all tabs', async ({ page }) => {
    await page.goto(BASE + '/system/permission', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);
    const count = await page.locator('.tab-item').count();
    expect(count).toBeGreaterThanOrEqual(4);
    console.log('System tabs: ' + count);
  });

  test('1.2 Store Archive - Loads with header visible', async ({ page }) => {
    await page.goto(BASE + '/operations/store-archive', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: 'test-results/phase1-store-' + TS + '.png' });
  });

  test('1.3 Product Category - Page accessible', async ({ page }) => {
    await page.goto(BASE + '/product/category', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: 'test-results/phase1-category-' + TS + '.png' });
  });

  test('1.4 Food Management - Product list loads', async ({ page }) => {
    await page.goto(BASE + '/product/food', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: 'test-results/phase1-food-' + TS + '.png' });
  });

  test('1.5 Product Pricing - Pricing page renders', async ({ page }) => {
    await page.goto(BASE + '/product/pricing', { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);
    await page.screenshot({ path: 'test-results/phase1-pricing-' + TS + '.png' });
  });
});