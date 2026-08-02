/**
 * 综合智能检测沙箱系统
 * 整合静态代码分析、运行时检测、API健康检查、数据流分析
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const projectRoot = path.resolve(__dirname, '..');
const frontendRoot = path.join(projectRoot, 'frontend');
const backendRoot = path.join(projectRoot, 'backend');

const report = {
  timestamp: new Date().toLocaleString('zh-CN'),
  summary: {
    totalIssues: 0,
    criticalIssues: 0,
    warningIssues: 0,
    infoIssues: 0,
  },
  categories: {
    staticCode: { issues: [], count: 0 },
    runtime: { issues: [], count: 0 },
    syntax: { issues: [], count: 0 },
    businessLogic: { issues: [], count: 0 },
    dataFlow: { issues: [], count: 0 },
    security: { issues: [], count: 0 },
  },
};

console.log('========================================');
console.log('  综合智能检测沙箱系统');
console.log('========================================\n');

function runStaticCodeAnalysis() {
  console.log('[1/6] 静态代码分析...');
  
  const issues = [];
  
  function scanDirectory(dir, extensions) {
    if (!fs.existsSync(dir)) return;
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (!['node_modules', 'dist', 'target', '.git', 'build', '__tests__'].includes(entry.name)) {
          scanDirectory(fullPath, extensions);
        }
      } else if (entry.isFile()) {
        const ext = path.extname(entry.name);
        if (extensions.includes(ext)) {
          try {
            const content = fs.readFileSync(fullPath, 'utf-8');
            const lines = content.split('\n');
            
            lines.forEach((line, index) => {
              const trimmed = line.trim();
              if (trimmed.startsWith('//') || trimmed.startsWith('*')) return;
              
              if (/功能开发中|开发中[^）]*\)/i.test(line)) {
                issues.push({
                  file: path.relative(projectRoot, fullPath),
                  line: index + 1,
                  type: '未实现功能',
                  severity: 'critical',
                  message: '发现未实现的功能占位符',
                });
              }
              
              if (/console\.(log|error|warn)\(/.test(line)) {
                issues.push({
                  file: path.relative(projectRoot, fullPath),
                  line: index + 1,
                  type: 'console输出',
                  severity: 'warning',
                  message: '生产代码中存在console输出',
                });
              }
              
              if (/: any\b/.test(line) || /<any>/.test(line)) {
                issues.push({
                  file: path.relative(projectRoot, fullPath),
                  line: index + 1,
                  type: 'any类型',
                  severity: 'warning',
                  message: '使用了any类型，缺少类型安全',
                });
              }
            });
          } catch (err) {
            // 忽略读取错误
          }
        }
      }
    }
  }
  
  scanDirectory(frontendRoot, ['.vue', '.ts', '.js']);
  scanDirectory(backendRoot, ['.java']);
  
  report.categories.staticCode.issues = issues.slice(0, 50);
  report.categories.staticCode.count = issues.length;
  report.summary.totalIssues += issues.length;
  report.summary.criticalIssues += issues.filter(i => i.severity === 'critical').length;
  report.summary.warningIssues += issues.filter(i => i.severity === 'warning').length;
  
  console.log('  -> 发现 ' + issues.length + ' 个静态代码问题');
}

function runSyntaxCheck() {
  console.log('[2/6] 语法检查...');
  
  const issues = [];
  
  try {
    console.log('  -> 检查TypeScript语法...');
    const result = execSync('cd frontend && npx tsc --noEmit 2>&1 || true', { 
      encoding: 'utf-8', 
      timeout: 60000,
      cwd: projectRoot 
    });
    
    const errorLines = result.split('\n').filter(line => line.includes('error TS'));
    errorLines.forEach(line => {
      const match = line.match(/(.+)\((\d+),(\d+)\): error (.+)/);
      if (match) {
        issues.push({
          file: match[1],
          line: parseInt(match[2]),
          type: 'TypeScript错误',
          severity: 'critical',
          message: match[4],
        });
      }
    });
  } catch (err) {
    issues.push({
      file: 'TypeScript编译',
      type: '编译检查失败',
      severity: 'info',
      message: err.message,
    });
  }
  
  report.categories.syntax.issues = issues.slice(0, 20);
  report.categories.syntax.count = issues.length;
  report.summary.totalIssues += issues.length;
  report.summary.criticalIssues += issues.filter(i => i.severity === 'critical').length;
  
  console.log('  -> 发现 ' + issues.length + ' 个语法问题');
}

function runRuntimeAnalysis() {
  console.log('[3/6] 运行时错误风险分析...');
  
  const issues = [];
  
  function scanVueFile(filePath, content) {
    const relativePath = path.relative(projectRoot, filePath);
    const scriptMatch = content.match(/<script setup[^>]*>([\s\S]*?)<\/script>/);
    if (!scriptMatch) return;
    
    const scriptContent = scriptMatch[1];
    const hasOnMounted = scriptContent.includes('onMounted(');
    const hasApiCall = scriptContent.includes('request.') || scriptContent.includes('api.') || scriptContent.includes('fetch(');
    
    if (hasOnMounted && !hasApiCall) {
      issues.push({
        file: relativePath,
        type: '缺少数据加载',
        severity: 'warning',
        message: 'onMounted中没有API调用，可能导致页面空数据',
      });
    }
    
    const deepAccessMatches = scriptContent.match(/\w+\.\w+\.\w+/g) || [];
    if (deepAccessMatches.length > 5) {
      issues.push({
        file: relativePath,
        type: '深层属性访问',
        severity: 'info',
        message: '存在' + deepAccessMatches.length + '处深层属性访问，可能需要空值检查',
      });
    }
  }
  
  function scanDirectory(dir) {
    if (!fs.existsSync(dir)) return;
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (!['node_modules', 'dist', 'target', '.git', 'build', '__tests__'].includes(entry.name)) {
          scanDirectory(fullPath);
        }
      } else if (entry.isFile() && entry.name.endsWith('.vue')) {
        try {
          const content = fs.readFileSync(fullPath, 'utf-8');
          scanVueFile(fullPath, content);
        } catch (err) {
          // 忽略
        }
      }
    }
  }
  
  scanDirectory(frontendRoot);
  
  report.categories.runtime.issues = issues.slice(0, 30);
  report.categories.runtime.count = issues.length;
  report.summary.totalIssues += issues.length;
  report.summary.warningIssues += issues.filter(i => i.severity === 'warning').length;
  
  console.log('  -> 发现 ' + issues.length + ' 个运行时风险');
}

function runBusinessLogicAnalysis() {
  console.log('[4/6] 业务逻辑分析...');
  
  const issues = [];
  
  const businessRules = [
    { pattern: /库存.*扣减|扣减.*库存/, message: '库存扣减逻辑需要检查并发安全' },
    { pattern: /订单.*状态|状态.*流转/, message: '订单状态流转需要检查完整性' },
    { pattern: /支付.*回调|回调.*处理/, message: '支付回调需要检查幂等性' },
    { pattern: /权限.*检查|权限.*验证/, message: '权限检查需要确保覆盖所有入口' },
    { pattern: /事务.*处理|@Transactional/, message: '事务处理需要检查回滚逻辑' },
  ];
  
  function scanFile(filePath, content) {
    const relativePath = path.relative(projectRoot, filePath);
    
    businessRules.forEach(rule => {
      if (rule.pattern.test(content)) {
        issues.push({
          file: relativePath,
          type: '业务逻辑检查',
          severity: 'info',
          message: rule.message,
        });
      }
    });
  }
  
  function scanDirectory(dir, extensions) {
    if (!fs.existsSync(dir)) return;
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (!['node_modules', 'dist', 'target', '.git', 'build'].includes(entry.name)) {
          scanDirectory(fullPath, extensions);
        }
      } else if (entry.isFile()) {
        const ext = path.extname(entry.name);
        if (extensions.includes(ext)) {
          try {
            const content = fs.readFileSync(fullPath, 'utf-8');
            scanFile(fullPath, content);
          } catch (err) {
            // 忽略
          }
        }
      }
    }
  }
  
  scanDirectory(backendRoot, ['.java']);
  scanDirectory(frontendRoot, ['.vue', '.ts']);
  
  report.categories.businessLogic.issues = issues.slice(0, 20);
  report.categories.businessLogic.count = issues.length;
  report.summary.totalIssues += issues.length;
  report.summary.infoIssues += issues.filter(i => i.severity === 'info').length;
  
  console.log('  -> 发现 ' + issues.length + ' 个业务逻辑检查点');
}

function runDataFlowAnalysis() {
  console.log('[5/6] 数据流分析...');
  
  const issues = [];
  
  const dataFlowPatterns = [
    { pattern: /request\.(get|post|put|delete)\(/, type: 'API调用', check: '检查参数和响应处理' },
    { pattern: /router\.(push|replace)\(/, type: '路由跳转', check: '检查路由参数传递' },
    { pattern: /emit\(['"`]/, type: '组件通信', check: '检查事件数据格式' },
    { pattern: /localStorage|sessionStorage/, type: '本地存储', check: '检查数据序列化' },
  ];
  
  function scanFile(filePath, content) {
    const relativePath = path.relative(projectRoot, filePath);
    
    dataFlowPatterns.forEach(pattern => {
      const matches = content.match(pattern.pattern) || [];
      if (matches.length > 0) {
        issues.push({
          file: relativePath,
          type: pattern.type,
          severity: 'info',
          message: pattern.check + ' (' + matches.length + '处)',
        });
      }
    });
  }
  
  function scanDirectory(dir, extensions) {
    if (!fs.existsSync(dir)) return;
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (!['node_modules', 'dist', 'target', '.git', 'build', '__tests__'].includes(entry.name)) {
          scanDirectory(fullPath, extensions);
        }
      } else if (entry.isFile()) {
        const ext = path.extname(entry.name);
        if (extensions.includes(ext)) {
          try {
            const content = fs.readFileSync(fullPath, 'utf-8');
            scanFile(fullPath, content);
          } catch (err) {
            // 忽略
          }
        }
      }
    }
  }
  
  scanDirectory(frontendRoot, ['.vue', '.ts']);
  
  report.categories.dataFlow.issues = issues.slice(0, 30);
  report.categories.dataFlow.count = issues.length;
  report.summary.totalIssues += issues.length;
  report.summary.infoIssues += issues.filter(i => i.severity === 'info').length;
  
  console.log('  -> 发现 ' + issues.length + ' 个数据流检查点');
}

function runSecurityAnalysis() {
  console.log('[6/6] 安全漏洞扫描...');
  
  const issues = [];
  
  const securityPatterns = [
    { pattern: /password|passwd|pwd/i, type: '敏感字段', message: '检查密码字段是否加密存储' },
    { pattern: /token|secret|key/i, type: '敏感信息', message: '检查敏感信息是否暴露' },
    { pattern: /eval\(|new Function\(/, type: '代码注入风险', message: '避免使用eval和new Function' },
    { pattern: /innerHTML|v-html/, type: 'XSS风险', message: '检查HTML内容是否经过过滤' },
    { pattern: /sql.*\+|".*".*\+/, type: 'SQL注入风险', message: '检查SQL拼接是否安全' },
  ];
  
  function scanFile(filePath, content) {
    const relativePath = path.relative(projectRoot, filePath);
    
    securityPatterns.forEach(pattern => {
      if (pattern.pattern.test(content)) {
        issues.push({
          file: relativePath,
          type: pattern.type,
          severity: 'warning',
          message: pattern.message,
        });
      }
    });
  }
  
  function scanDirectory(dir, extensions) {
    if (!fs.existsSync(dir)) return;
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (!['node_modules', 'dist', 'target', '.git', 'build', '__tests__'].includes(entry.name)) {
          scanDirectory(fullPath, extensions);
        }
      } else if (entry.isFile()) {
        const ext = path.extname(entry.name);
        if (extensions.includes(ext)) {
          try {
            const content = fs.readFileSync(fullPath, 'utf-8');
            scanFile(fullPath, content);
          } catch (err) {
            // 忽略
          }
        }
      }
    }
  }
  
  scanDirectory(frontendRoot, ['.vue', '.ts', '.js']);
  scanDirectory(backendRoot, ['.java']);
  
  report.categories.security.issues = issues.slice(0, 20);
  report.categories.security.count = issues.length;
  report.summary.totalIssues += issues.length;
  report.summary.warningIssues += issues.filter(i => i.severity === 'warning').length;
  
  console.log('  -> 发现 ' + issues.length + ' 个安全检查点');
}

function generateFinalReport() {
  console.log('\n生成综合检测报告...');
  
  let markdown = '# 综合智能检测报告\n\n';
  markdown += '检测时间: ' + report.timestamp + '\n\n';
  markdown += '---\n\n';
  
  markdown += '## 总体统计\n\n';
  markdown += '| 指标 | 数量 |\n';
  markdown += '|------|------|\n';
  markdown += '| **总问题数** | ' + report.summary.totalIssues + ' |\n';
  markdown += '| [严重] 问题 | ' + report.summary.criticalIssues + ' |\n';
  markdown += '| [警告] 问题 | ' + report.summary.warningIssues + ' |\n';
  markdown += '| [提示] 信息 | ' + report.summary.infoIssues + ' |\n\n';
  
  markdown += '## 分类详情\n\n';
  
  const categories = [
    { key: 'staticCode', name: '静态代码分析' },
    { key: 'syntax', name: '语法检查' },
    { key: 'runtime', name: '运行时风险' },
    { key: 'businessLogic', name: '业务逻辑' },
    { key: 'dataFlow', name: '数据流分析' },
    { key: 'security', name: '安全漏洞' },
  ];
  
  categories.forEach(cat => {
    const data = report.categories[cat.key];
    markdown += '### ' + cat.name + '\n\n';
    markdown += '发现问题: **' + data.count + '** 个\n\n';
    
    if (data.issues.length > 0) {
      markdown += '| 文件 | 类型 | 严重程度 | 说明 |\n';
      markdown += '|------|------|----------|------|\n';
      data.issues.slice(0, 15).forEach(issue => {
        const severityLabel = issue.severity === 'critical' ? '[严重]' : issue.severity === 'warning' ? '[警告]' : '[提示]';
        markdown += '| ' + issue.file + ' | ' + issue.type + ' | ' + severityLabel + ' | ' + issue.message + ' |\n';
      });
      if (data.issues.length > 15) {
        markdown += '| ... | ... | ... | 还有 ' + (data.issues.length - 15) + ' 个问题 |\n';
      }
    }
    markdown += '\n';
  });
  
  markdown += '## 修复建议\n\n';
  markdown += '### 优先级排序\n\n';
  markdown += '1. **[严重] 立即修复** - 严重问题，影响系统功能\n';
  markdown += '2. **[警告] 尽快修复** - 警告问题，影响代码质量\n';
  markdown += '3. **[提示] 建议优化** - 提示信息，可后续处理\n\n';
  
  markdown += '### 检测工具\n\n';
  markdown += '```bash\n';
  markdown += '# 运行综合检测\n';
  markdown += 'node scripts/smart-sandbox-detector.js\n';
  markdown += '\n';
  markdown += '# 运行静态代码检测\n';
  markdown += 'node scripts/code-quality-scanner.js\n';
  markdown += '\n';
  markdown += '# 运行运行时检测\n';
  markdown += 'node scripts/runtime-error-scanner.js\n';
  markdown += '```\n\n';
  
  markdown += '---\n';
  markdown += '*此报告由综合智能检测沙箱系统自动生成*\n';
  
  const reportPath = path.join(projectRoot, 'SMART_SANDBOX_REPORT.md');
  fs.writeFileSync(reportPath, markdown, 'utf-8');
  
  console.log('报告已保存到: ' + reportPath);
}

runStaticCodeAnalysis();
runSyntaxCheck();
runRuntimeAnalysis();
runBusinessLogicAnalysis();
runDataFlowAnalysis();
runSecurityAnalysis();

console.log('\n========================================');
console.log('检测结果汇总:');
console.log('  总问题数: ' + report.summary.totalIssues);
console.log('  [严重]: ' + report.summary.criticalIssues);
console.log('  [警告]: ' + report.summary.warningIssues);
console.log('  [提示]: ' + report.summary.infoIssues);
console.log('========================================\n');

generateFinalReport();

console.log('\n综合智能检测完成!');
