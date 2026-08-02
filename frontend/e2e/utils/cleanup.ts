import { Page } from '@playwright/test';
import { apiDelete } from './api-helper';

/**
 * 测试数据清理工具
 */

/**
 * 通过 API 删除测试中创建的记录
 * 调用后端通用删除接口
 */
export async function deleteTestRecord(page: Page, apiPath: string, id: string | number): Promise<void> {
  try {
    await apiDelete(page, http://localhost:8081/api/);
  } catch {
    // 清理失败不阻断测试
    console.warn([Cleanup] Failed to delete record: /);
  }
}

/**
 * 删除通过关键词搜索找到的测试记录
 */
export async function deleteRecordsByKeyword(page: Page, searchApi: string, deleteApi: string, keyword: string): Promise<void> {
  try {
    const result = await page.evaluate(
      async ({ url, kw }: { url: string; kw: string }) => {
        const res = await fetch(${url}?keyword=&pageSize=100, {
          headers: { 'Content-Type': 'application/json' },
        });
        return res.json();
      },
      { url: http://localhost:8081/api, kw: keyword }
    );
    const records = result?.data?.records || result?.data?.list || [];
    for (const record of records) {
      if (record.id) {
        await deleteTestRecord(page, deleteApi, record.id);
      }
    }
  } catch {
    console.warn([Cleanup] Failed to search/delete by keyword: );
  }
}
