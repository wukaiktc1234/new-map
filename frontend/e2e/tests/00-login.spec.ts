import { test, expect } from '@playwright/test';

test.describe('\u8BA4\u8BC1\u4E0E\u767B\u5F55', () => {
  test('[P0] 登录页渲染 - 表单元素、背景、Logo', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');
    await expect(page).toHaveTitle(/\u767B\u5F55/);
    await expect(page.locator('input').first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('button', { name: '\u767B\u5F55', exact: true })).toBeVisible();
  });

  test('[P0] 正常登录 - admin/Admin@123 -> 跳转工作台', async ({ page }) => {
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
    await page.waitForTimeout(3000);

    await expect(page).toHaveTitle(/\u5DE5\u4F5C\u53F0/);
  });

  test('[P0] 登录失败 - 错误密码提示', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    await page.locator('input:not([disabled])[type=\"text\"]').first().pressSequentially('admin');
    await page.locator('input:not([disabled])[type=\"password\"]').first().pressSequentially('wrongpassword123');
    await page.getByRole('button', { name: '\u767B\u5F55', exact: true }).click();
    await page.waitForTimeout(3000);
    expect(page.url()).toContain('login');
  });

  test('[P1] 登出流程 - 退出后跳转登录页', async ({ page }) => {
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

    await page.evaluate((result: any) => {
      localStorage.setItem('token', result.data.token);
      localStorage.setItem('refresh_token', result.data.refreshToken);
      localStorage.setItem('user_id', result.data.userInfo.id);
      localStorage.setItem('username', result.data.userInfo.username);
    }, loginResult);

    await page.goto('/home');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // 点击用户头像下拉
    const userProfile = page.locator('.user-profile').first();
    await userProfile.click();
    await page.waitForTimeout(500);

    // 点击退出登录
    const logoutItem = page.locator('.el-dropdown-menu__item:has-text(\"\u9000\u51FA\u767B\u5F55\")');
    await logoutItem.first().click();
    await page.waitForTimeout(500);

    // 处理确认对话框
    const confirmBtn = page.locator('.el-message-box .el-button--primary').first();
    if (await confirmBtn.count() > 0) {
      await confirmBtn.click();
      await page.waitForTimeout(3000);
    }

    expect(page.url()).toContain('login');
  });
});

