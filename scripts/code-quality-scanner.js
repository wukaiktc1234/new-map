/**
 * 代码质量自动化扫描工具
 * 用于检测前端和后端代码中的问题
 */

const fs = require('fs');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const frontendRoot = path.join(projectRoot, 'frontend');
const backendRoot = path.join(projectRoot, 'backend');

const issues = {
  frontend: {
    unimplemented: [],
    consoleLogs: [],
    mockData: [],
    anyTypes: [],
    directAxios: [],
    wrongResponseHandling: [],
    missingImports: [],
  },
  backend: {
    missingMappers: [],
    missingTableLogic: [],
    physicalDelete: [],
    missingFields: [],
  },
  summary: {
    totalIssues: 0,
    criticalIssues: 0,
    warningIssues: 0,
  }
};

function scanFile(filePath, content, patterns, category) {
  const lines = content.split('\n');
  lines.forEach((line, index) => {
    const trimmedLine = line.trim();
    if (trimmedLine.startsWith('//') || trimmedLine.startsWith('*') || trimmedLine.startsWith('/*')) {
      return;
    }
    patterns.forEach(pattern => {
      if (pattern.regex.test(line)) {
        issues.frontend[category].push({
          file: filePath,
          line: index + 1,
          content: line.trim(),
          type: pattern.type,
          severity: pattern.severity,
        });
      }
    });
  });
}

function scanFrontendFile(filePath, content) {
  const relativePath = path.relative(frontendRoot, filePath);
  
  if (relativePath.includes('__tests__') || relativePath.includes('.test.') || relativePath.includes('.spec.')) {
    return;
  }
  
  if (relativePath.includes('api/request.ts') || relativePath.includes('utils/request.ts')) {
    return;
  }
  
  const unimplementedPatterns = [
    { regex: /功能开发中|开发中[^）]*\)/i, type: '未实现功能', severity: 'critical' },
    { regex: /ElMessage\.(info|success|warning)\(['"`][^'"`]*开发中[^'"`]*['"`]\)/i, type: '占位消息', severity: 'critical' },
    { regex: /showToast\(['"].*开发中['"]\)/i, type: '占位消息', severity: 'critical' },
    { regex: /此处将显示[^，]*功能开发中/i, type: '占位文本', severity: 'critical' },
  ];

  const consolePatterns = [
    { regex: /console\.(log|error|warn|debug|info)\(/, type: 'console输出', severity: 'warning' },
  ];

  const mockPatterns = [
    { regex: /setTimeout\([^)]+,\s*\d+\)/, type: 'setTimeout模拟', severity: 'warning' },
    { regex: /mockData|MOCK_DATA|fakeData/i, type: 'Mock数据', severity: 'warning' },
    { regex: /\/\/\s*模拟|\/\/\s*mock/i, type: '模拟代码', severity: 'warning' },
  ];

  const anyTypePatterns = [
    { regex: /:\s*any\b/, type: 'any类型', severity: 'warning' },
    { regex: /<any>/, type: 'any类型断言', severity: 'warning' },
    { regex: /as\s+any/, type: 'any类型断言', severity: 'warning' },
  ];

  const directAxiosPatterns = [
    { regex: /import\s+axios\s+from\s+['"]axios['"]/, type: '直接使用axios', severity: 'critical' },
    { regex: /axios\.(get|post|put|delete|patch)\(/, type: '直接调用axios', severity: 'critical' },
  ];

  const wrongResponsePatterns = [
    { regex: /response\.data\.data/, type: '错误响应处理', severity: 'critical' },
    { regex: /res\.data\.data/, type: '错误响应处理', severity: 'critical' },
  ];

  scanFile(relativePath, content, unimplementedPatterns, 'unimplemented');
  scanFile(relativePath, content, consolePatterns, 'consoleLogs');
  scanFile(relativePath, content, mockPatterns, 'mockData');
  scanFile(relativePath, content, anyTypePatterns, 'anyTypes');
  scanFile(relativePath, content, directAxiosPatterns, 'directAxios');
  scanFile(relativePath, content, wrongResponsePatterns, 'wrongResponseHandling');
}

function scanBackendFile(filePath, content) {
  const relativePath = path.relative(backendRoot, filePath);
  
  if (filePath.endsWith('.java')) {
    const lines = content.split('\n');
    let hasTableLogic = false;
    let hasDeletedField = false;
    
    lines.forEach((line, index) => {
      if (line.includes('@TableLogic')) {
        hasTableLogic = true;
      }
      if (line.includes('private') && line.includes('deleted')) {
        hasDeletedField = true;
      }
      if (line.includes('DELETE FROM') || line.includes('delete from')) {
        if (!line.includes('//') && !line.includes('/*')) {
          issues.backend.physicalDelete.push({
            file: relativePath,
            line: index + 1,
            content: line.trim(),
            type: '物理删除',
            severity: 'critical',
          });
        }
      }
    });
    
    if (hasDeletedField && !hasTableLogic && relativePath.includes('/entity/')) {
      issues.backend.missingTableLogic.push({
        file: relativePath,
        type: '缺少@TableLogic注解',
        severity: 'warning',
      });
    }
  }
}

function scanDirectory(dir, fileHandler, extensions) {
  if (!fs.existsSync(dir)) {
    console.log(`目录不存在: ${dir}`);
    return;
  }

  const entries = fs.readdirSync(dir, { withFileTypes: true });
  
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    
    if (entry.isDirectory()) {
      if (!['node_modules', 'dist', 'target', '.git', 'build'].includes(entry.name)) {
        scanDirectory(fullPath, fileHandler, extensions);
      }
    } else if (entry.isFile()) {
      const ext = path.extname(entry.name);
      if (extensions.includes(ext)) {
        try {
          const content = fs.readFileSync(fullPath, 'utf-8');
          fileHandler(fullPath, content);
        } catch (err) {
          console.error(`无法读取文件: ${fullPath}`, err.message);
        }
      }
    }
  }
}

function checkMapperFiles() {
  const mapperJavaDir = path.join(backendRoot, 'src/main/java/com/example/demo/mapper');
  const mapperXmlDir = path.join(backendRoot, 'src/main/resources/mapper');
  
  if (!fs.existsSync(mapperJavaDir)) {
    console.log('Mapper Java目录不存在');
    return;
  }

  const javaMappers = fs.readdirSync(mapperJavaDir)
    .filter(f => f.endsWith('.java'))
    .map(f => f.replace('.java', ''));
  
  if (fs.existsSync(mapperXmlDir)) {
    const xmlMappers = fs.readdirSync(mapperXmlDir)
      .filter(f => f.endsWith('.xml'))
      .map(f => f.replace('.xml', ''));
    
    javaMappers.forEach(mapper => {
      if (!xmlMappers.includes(mapper)) {
        issues.backend.missingMappers.push({
          file: `${mapper}.java`,
          type: '缺少Mapper XML文件',
          severity: 'critical',
        });
      }
    });
  } else {
    javaMappers.forEach(mapper => {
      issues.backend.missingMappers.push({
        file: `${mapper}.java`,
        type: 'Mapper XML目录不存在',
        severity: 'critical',
      });
    });
  }
}

function generateReport() {
  let report = '# 代码质量扫描报告\n\n';
  report += `扫描时间: ${new Date().toLocaleString('zh-CN')}\n\n`;
  
  const frontendUnimplemented = issues.frontend.unimplemented.length;
  const frontendConsole = issues.frontend.consoleLogs.length;
  const frontendMock = issues.frontend.mockData.length;
  const frontendAny = issues.frontend.anyTypes.length;
  const frontendAxios = issues.frontend.directAxios.length;
  const frontendResponse = issues.frontend.wrongResponseHandling.length;
  const backendMappers = issues.backend.missingMappers.length;
  const backendTableLogic = issues.backend.missingTableLogic.length;
  const backendDelete = issues.backend.physicalDelete.length;

  issues.summary.totalIssues = frontendUnimplemented + frontendConsole + frontendMock + 
                               frontendAny + frontendAxios + frontendResponse +
                               backendMappers + backendTableLogic + backendDelete;
  issues.summary.criticalIssues = frontendUnimplemented + frontendAxios + frontendResponse +
                                  backendMappers + backendDelete;
  issues.summary.warningIssues = frontendConsole + frontendMock + frontendAny + backendTableLogic;

  report += '## 📊 问题统计\n\n';
  report += '| 类别 | 数量 | 严重程度 |\n';
  report += '|------|------|----------|\n';
  report += `| 🔴 未实现功能/占位符 | ${frontendUnimplemented} | 严重 |\n`;
  report += `| 🔴 直接使用axios | ${frontendAxios} | 严重 |\n`;
  report += `| 🔴 错误响应处理 | ${frontendResponse} | 严重 |\n`;
  report += `| 🔴 缺少Mapper XML | ${backendMappers} | 严重 |\n`;
  report += `| 🔴 物理删除 | ${backendDelete} | 严重 |\n`;
  report += `| 🟡 console输出 | ${frontendConsole} | 警告 |\n`;
  report += `| 🟡 Mock数据/模拟 | ${frontendMock} | 警告 |\n`;
  report += `| 🟡 any类型 | ${frontendAny} | 警告 |\n`;
  report += `| 🟡 缺少@TableLogic | ${backendTableLogic} | 警告 |\n`;
  report += '| **总计** | **' + issues.summary.totalIssues + '** | - |\n\n';

  if (issues.frontend.unimplemented.length > 0) {
    report += '## 🔴 未实现功能详情\n\n';
    const grouped = {};
    issues.frontend.unimplemented.forEach(item => {
      if (!grouped[item.file]) grouped[item.file] = [];
      grouped[item.file].push(item);
    });
    
    Object.entries(grouped).forEach(([file, items]) => {
      report += `### ${file}\n\n`;
      items.slice(0, 5).forEach(item => {
        report += `- 第${item.line}行: \`${item.content.substring(0, 80)}${item.content.length > 80 ? '...' : ''}\`\n`;
      });
      if (items.length > 5) {
        report += `- ... 还有 ${items.length - 5} 处\n`;
      }
      report += '\n';
    });
  }

  if (issues.frontend.directAxios.length > 0) {
    report += '## 🔴 直接使用axios详情\n\n';
    issues.frontend.directAxios.forEach(item => {
      report += `- ${item.file}:${item.line} - ${item.content.substring(0, 60)}\n`;
    });
    report += '\n';
  }

  if (issues.frontend.wrongResponseHandling.length > 0) {
    report += '## 🔴 错误响应处理详情\n\n';
    issues.frontend.wrongResponseHandling.forEach(item => {
      report += `- ${item.file}:${item.line}\n`;
    });
    report += '\n';
  }

  if (issues.backend.missingMappers.length > 0) {
    report += '## 🔴 缺少Mapper XML文件\n\n';
    issues.backend.missingMappers.forEach(item => {
      report += `- ${item.file} - ${item.type}\n`;
    });
    report += '\n';
  }

  if (issues.frontend.consoleLogs.length > 0) {
    report += '## 🟡 Console输出详情 (前20条)\n\n';
    issues.frontend.consoleLogs.slice(0, 20).forEach(item => {
      report += `- ${item.file}:${item.line}\n`;
    });
    if (issues.frontend.consoleLogs.length > 20) {
      report += `- ... 还有 ${issues.frontend.consoleLogs.length - 20} 处\n`;
    }
    report += '\n';
  }

  if (issues.frontend.mockData.length > 0) {
    report += '## 🟡 Mock数据详情 (前20条)\n\n';
    issues.frontend.mockData.slice(0, 20).forEach(item => {
      report += `- ${item.file}:${item.line} - ${item.type}\n`;
    });
    if (issues.frontend.mockData.length > 20) {
      report += `- ... 还有 ${issues.frontend.mockData.length - 20} 处\n`;
    }
    report += '\n';
  }

  report += '## 📋 修复优先级建议\n\n';
  report += '1. **立即修复 (严重)**\n';
  report += '   - 所有未实现功能/占位符消息\n';
  report += '   - 直接使用axios的代码\n';
  report += '   - 错误的响应数据处理\n';
  report += '   - 缺少的Mapper XML文件\n';
  report += '   - 物理删除语句\n\n';
  report += '2. **后续优化 (警告)**\n';
  report += '   - 移除console.log调试代码\n';
  report += '   - 替换Mock数据为真实API\n';
  report += '   - 替换any类型为具体类型\n';
  report += '   - 添加@TableLogic注解\n\n';

  report += '---\n';
  report += '*此报告由代码质量扫描工具自动生成*\n';

  return report;
}

console.log('🔍 开始扫描前端代码...');
scanDirectory(frontendRoot, scanFrontendFile, ['.vue', '.ts', '.js']);

console.log('🔍 开始扫描后端代码...');
scanDirectory(backendRoot, scanBackendFile, ['.java']);

console.log('🔍 检查Mapper文件...');
checkMapperFiles();

console.log('📝 生成报告...');
const report = generateReport();

const reportPath = path.join(projectRoot, 'CODE_QUALITY_REPORT.md');
fs.writeFileSync(reportPath, report, 'utf-8');

console.log('\n✅ 扫描完成!');
console.log(`📊 总问题数: ${issues.summary.totalIssues}`);
console.log(`🔴 严重问题: ${issues.summary.criticalIssues}`);
console.log(`🟡 警告问题: ${issues.summary.warningIssues}`);
console.log(`📄 报告已保存到: ${reportPath}`);

module.exports = { issues, generateReport };
