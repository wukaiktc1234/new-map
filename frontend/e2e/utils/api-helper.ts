import { Page } from '@playwright/test';

/**
 * API 辅助工具
 * 通过前端页面直接调用 API（使用已登录的 cookie/token）
 */

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

/**
 * 在浏览器上下文中执行 API 请求
 * 自动携带已登录的 cookie 和 token
 */
export async function apiGet<T>(page: Page, url: string): Promise<ApiResponse<T>> {
  const result = await page.evaluate(async (apiUrl: string) => {
    const res = await fetch(apiUrl, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });
    return res.json();
  }, url);
  return result as ApiResponse<T>;
}

export async function apiPost<T>(page: Page, url: string, body: unknown = {}): Promise<ApiResponse<T>> {
  const result = await page.evaluate(
    async ({ apiUrl, data }: { apiUrl: string; data: unknown }) => {
      const res = await fetch(apiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      return res.json();
    },
    { apiUrl: url, data: body }
  );
  return result as ApiResponse<T>;
}

export async function apiPut<T>(page: Page, url: string, body: unknown = {}): Promise<ApiResponse<T>> {
  const result = await page.evaluate(
    async ({ apiUrl, data }: { apiUrl: string; data: unknown }) => {
      const res = await fetch(apiUrl, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      return res.json();
    },
    { apiUrl: url, data: body }
  );
  return result as ApiResponse<T>;
}

export async function apiDelete<T>(page: Page, url: string): Promise<ApiResponse<T>> {
  const result = await page.evaluate(async (apiUrl: string) => {
    const res = await fetch(apiUrl, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
    });
    return res.json();
  }, url);
  return result as ApiResponse<T>;
}
