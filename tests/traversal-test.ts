/**
 * 管理端全功能遍历测试
 * 使用 Playwright 通过API登录并遍历所有路由，收集页面错误
 *
 * 策略：
 * 1. 调用后端 /v1/auth/login API 获取真实JWT Token
 * 2. 使用 page.addInitScript 在每个页面加载前预设 localStorage 的 token
 * 3. 路由守卫会调用 initFromToken() 从 localStorage 读取 token 初始化 Pinia store
 * 4. 使用 page.goto 导航到每个路由，等待页面加载完成
 */
import { chromium, type Browser, type Page } from 'playwright';
import * as fs from 'fs';
import * as path from 'path';
import { routes, type RouteItem } from './routes';

const BASE_URL = 'http://localhost:3002'; // 管理端实际端口是3002（3001是POS端）
const API_BASE = 'http://localhost:8081/api';
const USERNAME = 'admin';
const PASSWORD = 'Admin@123';
const REPORT_DIR = path.join(__dirname, '..', 'test-report');
const SCREENSHOT_DIR = path.join(REPORT_DIR, 'screenshots');

interface LoginResult {
  token: string;
  refreshToken?: string;
  userInfo: {
    id: string;
    username: string;
    roles: string[];
    permissions: string[];
  };
}

interface PageIssue {
  route: string;
  title: string;
  status: 'ok' | 'warning' | 'error';
  consoleErrors: string[];
  networkErrors: { url: string; status: number; method: string }[];
  vueErrors: string[];
  isBlank: boolean;
  blankReason?: string;
  loadTime: number;
  screenshotPath?: string;
  bodyTextLength: number;
  hasElMessage?: boolean;
  elMessageText?: string;
  finalUrl?: string;
  redirectedToLogin?: boolean;
  httpStatus?: number;
}

/**
 * 通过后端API登录，获取真实JWT Token
 */
async function loginViaApi(): Promise<LoginResult> {
  console.log('正在通过API登录管理端...');

  const response = await fetch(`${API_BASE}/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USERNAME, password: PASSWORD }),
  });

  if (!response.ok) {
    throw new Error(`登录API请求失败: HTTP ${response.status}`);
  }

  const json = await response.json();
  if (json.code !== 0 || !json.data || !json.data.token) {
    throw new Error(`登录失败: ${json.message || JSON.stringify(json).substring(0, 200)}`);
  }

  const data = json.data;
  console.log(`登录成功！用户: ${data.userInfo.username}, 角色: [${data.userInfo.roles.join(', ')}]`);
  console.log(`Token长度: ${data.token.length}`);

  return {
    token: data.token,
    refreshToken: data.refreshToken,
    userInfo: data.userInfo,
  };
}

/**
 * 在页面上预设localStorage的token，确保路由守卫能正确初始化Pinia store
 */
async function setupAuth(page: Page, loginResult: LoginResult): Promise<void> {
  // 使用 addInitScript 在每个页面加载前注入token
  // 这样无论页面如何刷新，token都会被预设
  await page.addInitScript((auth) => {
    localStorage.setItem('token', auth.token);
    if (auth.refreshToken) {
      localStorage.setItem('refresh_token', auth.refreshToken);
    }
    localStorage.setItem('user_id', auth.userInfo.id);
    localStorage.setItem('username', auth.userInfo.username);
    localStorage.setItem('roles', JSON.stringify(auth.userInfo.roles));
    localStorage.setItem('permissions', JSON.stringify(auth.userInfo.permissions));
  }, loginResult);
}

/**
 * 遍历单个路由，收集问题
 */
async function testRoute(page: Page, route: RouteItem): Promise<PageIssue> {
  const issue: PageIssue = {
    route: route.path,
    title: route.title,
    status: 'ok',
    consoleErrors: [],
    networkErrors: [],
    vueErrors: [],
    isBlank: false,
    loadTime: 0,
    bodyTextLength: 0,
  };

  const consoleErrors: string[] = [];
  const networkErrors: { url: string; status: number; method: string }[] = [];
  const vueErrors: string[] = [];

  // 监听控制台错误
  const consoleHandler = (msg: { type(): string; text(): string }) => {
    if (msg.type() === 'error') {
      const text = msg.text();
      // 过滤掉 favicon 和浏览器扩展相关的噪声
      if (!text.includes('favicon') && !text.includes('extension') && !text.includes('DevTools')) {
        consoleErrors.push(text);
      }
    }
  };
  page.on('console', consoleHandler);

  // 监听页面错误（Vue渲染错误等）
  const pageErrorHandler = (err: Error) => {
    vueErrors.push(err.message);
  };
  page.on('pageerror', pageErrorHandler);

  // 监听网络请求失败
  const responseHandler = (response: { status(): number; url(): string; request(): { method(): string }; ok(): boolean }) => {
    if (!response.ok() && response.status() >= 400) {
      const url = response.url();
      // 过滤掉 favicon 和非API请求
      if (url.includes('/api/') || url.includes('/v1/')) {
        networkErrors.push({
          url: url,
          status: response.status(),
          method: response.request().method(),
        });
      }
    }
  };
  page.on('response', responseHandler);

  const startTime = Date.now();

  try {
    // 使用 page.goto 导航到目标路由
    // addInitScript 会在每个页面加载前注入 token，确保路由守卫能正确初始化
    const response = await page.goto(`${BASE_URL}${route.path}`, {
      waitUntil: 'networkidle',
      timeout: 30000,
    });

    issue.loadTime = Date.now() - startTime;
    issue.httpStatus = response?.status();
    issue.finalUrl = page.url();

    // 等待Vue应用挂载和异步数据加载
    await page.waitForTimeout(2000);

    // 检测是否被重定向到登录页
    if (issue.finalUrl.includes('/login')) {
      issue.redirectedToLogin = true;
      issue.status = 'error';
      issue.blankReason = '被重定向到登录页（token无效或权限不足）';
    }

    // 检查页面内容
    const bodyText = await page.evaluate(() => {
      const body = document.body;
      return body ? body.innerText.trim() : '';
    });
    issue.bodyTextLength = bodyText.length;

    // 白屏检测：排除登录页重定向的情况
    if (bodyText.length < 50 && !issue.redirectedToLogin) {
      issue.isBlank = true;
      issue.blankReason = `页面内容过少 (${bodyText.length}字符)`;
      issue.status = 'error';
    }

    // 检查是否有 Element Plus 错误消息
    const elMessage = page.locator('.el-message--error').first();
    if (await elMessage.isVisible().catch(() => false)) {
      issue.hasElMessage = true;
      issue.elMessageText = await elMessage.textContent().catch(() => '') || '';
      if (issue.status !== 'error') issue.status = 'warning';
    }

    // 检查是否有"加载失败"等错误提示
    // 注意：移除纯数字 '500'/'404' 关键词，避免与业务数据（价格、编码）误匹配
    const errorTexts = ['加载失败', '请求失败', '服务器错误', '系统异常', 'Network Error', 'HTTP 500', 'HTTP 404', '错误码: 500', '错误码: 404'];
    const hasErrorText = errorTexts.some(t => bodyText.includes(t));
    if (hasErrorText && issue.status !== 'error') {
      issue.status = 'warning';
    }

    // 截图（仅对有问题的页面）
    if (issue.status !== 'ok' || consoleErrors.length > 0 || networkErrors.length > 0) {
      const screenshotPath = path.join(SCREENSHOT_DIR, `${route.path.replace(/\//g, '_')}.png`);
      await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
      issue.screenshotPath = screenshotPath;
    }

  } catch (err) {
    issue.loadTime = Date.now() - startTime;
    issue.status = 'error';
    issue.isBlank = true;
    issue.blankReason = `导航失败: ${(err as Error).message.substring(0, 200)}`;
    const screenshotPath = path.join(SCREENSHOT_DIR, `${route.path.replace(/\//g, '_')}_error.png`);
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
    issue.screenshotPath = screenshotPath;
  }

  issue.consoleErrors = consoleErrors;
  issue.networkErrors = networkErrors;
  issue.vueErrors = vueErrors;

  if (consoleErrors.length > 0 && issue.status !== 'error') issue.status = 'warning';
  if (networkErrors.length > 0 && issue.status !== 'error') issue.status = 'warning';
  if (vueErrors.length > 0 && issue.status !== 'error') issue.status = 'warning';

  // 移除监听器
  page.off('console', consoleHandler);
  page.off('pageerror', pageErrorHandler);
  page.off('response', responseHandler);

  return issue;
}

/**
 * 生成测试报告
 */
function generateReport(issues: PageIssue[]): void {
  if (!fs.existsSync(REPORT_DIR)) {
    fs.mkdirSync(REPORT_DIR, { recursive: true });
  }

  // JSON 详细报告
  fs.writeFileSync(
    path.join(REPORT_DIR, 'test-report.json'),
    JSON.stringify(issues, null, 2),
    'utf-8'
  );

  // 汇总统计
  const total = issues.length;
  const ok = issues.filter(i => i.status === 'ok').length;
  const warning = issues.filter(i => i.status === 'warning').length;
  const error = issues.filter(i => i.status === 'error').length;

  // Markdown 报告
  let md = `# 管理端全功能遍历测试报告\n\n`;
  md += `**测试时间**: ${new Date().toLocaleString('zh-CN')}\n\n`;
  md += `**测试页面总数**: ${total}\n\n`;
  md += `**测试账号**: admin\n\n`;
  md += `## 测试结果汇总\n\n`;
  md += `| 状态 | 数量 | 占比 |\n|------|------|------|\n`;
  md += `| ✅ 正常 | ${ok} | ${(ok/total*100).toFixed(1)}% |\n`;
  md += `| ⚠️ 警告 | ${warning} | ${(warning/total*100).toFixed(1)}% |\n`;
  md += `| ❌ 错误 | ${error} | ${(error/total*100).toFixed(1)}% |\n\n`;

  // 错误页面详情
  if (error > 0) {
    md += `## ❌ 错误页面（${error}个）\n\n`;
    issues.filter(i => i.status === 'error').forEach(i => {
      md += `### ${i.title} (${i.route})\n\n`;
      md += `- **加载时间**: ${i.loadTime}ms\n`;
      md += `- **HTTP状态**: ${i.httpStatus || '未知'}\n`;
      md += `- **最终URL**: ${i.finalUrl || '未知'}\n`;
      md += `- **白屏**: ${i.isBlank ? '是' : '否'}${i.blankReason ? ' - ' + i.blankReason : ''}\n`;
      md += `- **内容长度**: ${i.bodyTextLength} 字符\n`;
      if (i.redirectedToLogin) md += `- **⚠️ 重定向到登录页**: 是\n`;
      if (i.networkErrors.length > 0) {
        md += `- **网络错误**:\n`;
        i.networkErrors.forEach(e => md += `  - [${e.method}] ${e.url} → ${e.status}\n`);
      }
      if (i.consoleErrors.length > 0) {
        md += `- **控制台错误**:\n`;
        i.consoleErrors.slice(0, 5).forEach(e => md += `  - ${e}\n`);
        if (i.consoleErrors.length > 5) md += `  - ... 还有 ${i.consoleErrors.length - 5} 条\n`;
      }
      if (i.vueErrors.length > 0) {
        md += `- **Vue错误**:\n`;
        i.vueErrors.slice(0, 3).forEach(e => md += `  - ${e}\n`);
      }
      if (i.elMessageText) md += `- **页面错误提示**: ${i.elMessageText}\n`;
      md += `\n`;
    });
  }

  // 警告页面详情
  if (warning > 0) {
    md += `## ⚠️ 警告页面（${warning}个）\n\n`;
    md += `| 页面 | 路由 | 网络错误 | 控制台错误 | Vue错误 | 错误提示 |\n`;
    md += `|------|------|----------|-----------|---------|----------|\n`;
    issues.filter(i => i.status === 'warning').forEach(i => {
      md += `| ${i.title} | ${i.route} | ${i.networkErrors.length} | ${i.consoleErrors.length} | ${i.vueErrors.length} | ${i.elMessageText || '-'} |\n`;
    });
    md += `\n`;

    // 警告页面详细网络错误
    md += `### 警告页面网络错误详情\n\n`;
    issues.filter(i => i.status === 'warning' && i.networkErrors.length > 0).forEach(i => {
      md += `**${i.title}** (${i.route}):\n`;
      i.networkErrors.forEach(e => md += `- [${e.method}] ${e.url} → ${e.status}\n`);
      md += `\n`;
    });
  }

  // 正常页面列表
  if (ok > 0) {
    md += `## ✅ 正常页面（${ok}个）\n\n`;
    md += `| 页面 | 路由 | 加载时间 | 内容长度 |\n|------|------|----------|----------|\n`;
    issues.filter(i => i.status === 'ok').forEach(i => {
      md += `| ${i.title} | ${i.route} | ${i.loadTime}ms | ${i.bodyTextLength}字 |\n`;
    });
  }

  fs.writeFileSync(path.join(REPORT_DIR, 'test-report.md'), md, 'utf-8');

  // 控制台输出汇总
  console.log('\n========== 测试结果汇总 ==========');
  console.log(`总页面数: ${total}`);
  console.log(`✅ 正常: ${ok} (${(ok/total*100).toFixed(1)}%)`);
  console.log(`⚠️ 警告: ${warning} (${(warning/total*100).toFixed(1)}%)`);
  console.log(`❌ 错误: ${error} (${(error/total*100).toFixed(1)}%)`);
  console.log(`\n报告已生成:`);
  console.log(`  - ${path.join(REPORT_DIR, 'test-report.md')}`);
  console.log(`  - ${path.join(REPORT_DIR, 'test-report.json')}`);
  console.log(`==================================\n`);
}

async function main(): Promise<void> {
  // 创建截图目录
  if (!fs.existsSync(SCREENSHOT_DIR)) {
    fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
  }

  console.log(`启动测试，共 ${routes.length} 个页面`);

  // 1. 通过API登录获取token
  const loginResult = await loginViaApi();

  // 2. 使用系统安装的 Chrome 浏览器
  const browser: Browser = await chromium.launch({ headless: true, channel: 'chrome' });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    locale: 'zh-CN',
  });
  const page = await context.newPage();

  // 3. 预设认证信息
  await setupAuth(page, loginResult);

  // 4. 遍历所有路由
  const issues: PageIssue[] = [];
  for (let i = 0; i < routes.length; i++) {
    const route = routes[i];
    process.stdout.write(`[${i + 1}/${routes.length}] 测试: ${route.title} (${route.path}) ... `);
    const issue = await testRoute(page, route);
    issues.push(issue);
    const statusIcon = issue.status === 'ok' ? '✅' : issue.status === 'warning' ? '⚠️' : '❌';
    const redirectInfo = issue.redirectedToLogin ? ' [重定向登录!]' : '';
    const blankInfo = issue.isBlank && !issue.redirectedToLogin ? ` [白屏:${issue.bodyTextLength}字]` : '';
    const netInfo = issue.networkErrors.length > 0 ? ` [网络错误:${issue.networkErrors.length}]` : '';
    console.log(`${statusIcon} ${issue.loadTime}ms${redirectInfo}${blankInfo}${netInfo}`);
  }

  // 5. 生成报告
  generateReport(issues);

  await browser.close();
}

main().catch(err => {
  console.error('测试执行失败:', err);
  process.exit(1);
});
