/**
 * 运行时错误检测工具
 * 用于检测Vue组件中常见的运行时错误
 */

const fs = require('fs');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const frontendRoot = path.join(projectRoot, 'frontend');

const issues = {
  missingDataLoading: [],
  unsafePropertyAccess: [],
  missingNullCheck: [],
  undefinedMethod: [],
  missingApiImport: [],
};

function scanVueFile(filePath, content) {
  const relativePath = path.relative(frontendRoot, filePath);
  
  const hasOnMounted = content.includes('onMounted(');
  const hasApiImport = content.includes("from '@/api") || content.includes('request.get') || content.includes('request.post');
  const hasLoadingRef = content.includes('loading') && content.includes('ref(');
  
  const scriptMatch = content.match(/<script setup[^>]*>([\s\S]*?)<\/script>/);
  if (!scriptMatch) return;
  
  const scriptContent = scriptMatch[1];
  
  const hasDataFetch = 
    scriptContent.includes('fetch') || 
    scriptContent.includes('load') || 
    scriptContent.includes('get') ||
    scriptContent.includes('api.') ||
    scriptContent.includes('request.');
  
  if (hasOnMounted && !hasDataFetch && !hasApiImport) {
    issues.missingDataLoading.push({
      file: relativePath,
      type: 'onMounted中没有数据加载',
      severity: 'warning',
    });
  }
  
  const unsafePatterns = [
    { regex: /\.value\s*\.\s*\w+\s*\(/g, type: '可能未定义的方法调用' },
    { regex: /\?\.\w+\s*\(/g, type: '可选链后调用方法' },
    { regex: /\w+\s*\.\s*\w+\s*\.\s*\w+/g, type: '深层属性访问' },
  ];
  
  unsafePatterns.forEach(pattern => {
    const matches = scriptContent.match(pattern.regex);
    if (matches && matches.length > 3) {
      issues.unsafePropertyAccess.push({
        file: relativePath,
        type: pattern.type,
        count: matches.length,
      });
    }
  });
  
  const vForMatches = content.match(/v-for="[^"]*"/g) || [];
  vForMatches.forEach(match => {
    if (!content.includes(':key=') && !content.includes('v-bind:key')) {
      issues.missingNullCheck.push({
        file: relativePath,
        type: 'v-for缺少key',
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
          scanVueFile(fullPath, content);
        } catch (err) {
          console.error(`无法读取文件: ${fullPath}`, err.message);
        }
      }
    }
  }
}

function generateReport() {
  let report = '# 运行时错误检测报告\n\n';
  report += `扫描时间: ${new Date().toLocaleString('zh-CN')}\n\n`;
  
  report += '## 📊 问题统计\n\n';
  report += '| 类别 | 数量 |\n';
  report += '|------|------|\n';
  report += `| 缺少数据加载 | ${issues.missingDataLoading.length} |\n`;
  report += `| 不安全属性访问 | ${issues.unsafePropertyAccess.length} |\n`;
  report += `| 缺少key检查 | ${issues.missingNullCheck.length} |\n`;
  report += '| **总计** | **' + (issues.missingDataLoading.length + issues.unsafePropertyAccess.length + issues.missingNullCheck.length) + '** |\n\n';

  if (issues.missingDataLoading.length > 0) {
    report += '## ⚠️ 缺少数据加载的页面\n\n';
    report += '以下页面在 onMounted 中没有数据加载逻辑，可能导致页面显示空数据：\n\n';
    issues.missingDataLoading.forEach(item => {
      report += `- ${item.file}\n`;
    });
    report += '\n';
  }

  if (issues.unsafePropertyAccess.length > 0) {
    report += '## ⚠️ 不安全属性访问\n\n';
    report += '以下文件可能存在未定义属性访问的风险：\n\n';
    issues.unsafePropertyAccess.forEach(item => {
      report += `- ${item.file} (${item.type}: ${item.count}处)\n`;
    });
    report += '\n';
  }

  report += '## 📋 修复建议\n\n';
  report += '1. **添加数据加载**: 在 onMounted 中调用API加载数据\n';
  report += '2. **添加空值检查**: 使用可选链操作符 `?.` 或 `v-if` 检查数据是否存在\n';
  report += '3. **添加加载状态**: 使用 `v-loading` 显示加载状态\n';
  report += '4. **错误处理**: 添加 try-catch 捕获API错误\n\n';

  report += '---\n';
  report += '*此报告由运行时错误检测工具自动生成*\n';

  return report;
}

console.log('🔍 开始扫描运行时错误风险...');

scanDirectory(frontendRoot, ['.vue']);

console.log('📝 生成报告...');

const report = generateReport();
const reportPath = path.join(projectRoot, 'RUNTIME_ERROR_REPORT.md');
fs.writeFileSync(reportPath, report, 'utf-8');

console.log('\n✅ 扫描完成!');
console.log(`📊 发现 ${issues.missingDataLoading.length + issues.unsafePropertyAccess.length + issues.missingNullCheck.length} 个潜在问题`);
console.log(`📄 报告已保存到: ${reportPath}`);

module.exports = { issues };
