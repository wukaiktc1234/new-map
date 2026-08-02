import { test as setup, expect } from '@playwright/test';

const AUTH_FILE = 'e2e/.auth/user.json';

setup('\u5168\u5C40\u767B\u5F55 - admin', async ({ page }) => {
  await page.goto('/login');
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(1000);

  const loginResult = await page.evaluate(async () => {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'Admin@123' }),
    });
    return res.json();
  });

  expect(loginResult.code).toBe(0);

  await page.evaluate((result: any) => {
    localStorage.setItem('token', result.data.token);
    localStorage.setItem('refresh_token', result.data.refreshToken);
    localStorage.setItem('user_id', result.data.userInfo.id);
    localStorage.setItem('username', result.data.userInfo.username);
  }, loginResult);

  await page.goto('/home');
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(2000);

  await page.context().storageState({ path: AUTH_FILE });
});

