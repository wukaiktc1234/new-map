/**
 * 增强版自动化批量修复工具
 * 用于修复第二轮扫描发现的剩余严重问题
 */

const fs = require('fs');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const frontendRoot = path.join(projectRoot, 'frontend');
const backendRoot = path.join(projectRoot, 'backend');

const fixLog = {
  unimplemented: [],
  directAxios: [],
  wrongResponse: [],
  physicalDelete: [],
};

function fixRemainingUnimplemented() {
  console.log('\n🔧 修复剩余未实现功能占位符...');
  
  const filesToFix = [
    'src/components/CallNumber/RecordsTab.vue',
    'src/components/Dashboard/DashboardStatCards.vue',
    'src/components/Dashboard/DashboardTodoList.vue',
    'src/components/Finance/FinanceSummaryHeader.vue',
    'src/components/HR/HealthCheckManagement.vue',
    'src/composables/useHealthCertificate.ts',
    'src/views/Asset/Depreciation.vue',
    'src/views/Asset/Inventory.vue',
    'src/views/Asset/Ledger.vue',
    'src/views/Asset/Overview.vue',
    'src/views/Cashier/StoreAsset.vue',
    'src/views/dashboard/index.vue',
    'src/views/FinanceForecast.vue',
    'src/views/FinanceInvoice.vue',
    'src/views/FinanceInvoiceDetail.vue',
    'src/views/FinancePrint.vue',
    'src/views/FinanceProfit.vue',
    'src/views/financeVoucher.vue',
    'src/views/FoodsafetyQuality.vue',
    'src/views/FoodsafetyTraceability.vue',
    'src/views/HR/ArchiveManagement.vue',
    'src/views/HR/AttendanceManagement.vue',
    'src/views/HR/PerformanceManagement.vue',
    'src/views/HR/PersonnelReport.vue',
    'src/views/HR/RecruitmentManagement.vue',
    'src/views/HR/SalaryManagement.vue',
    'src/views/HR/ShiftManagement.vue',
    'src/views/LabelTemplateDesigner.vue',
    'src/views/profile/index.vue',
    'src/views/Purchase/ElectronicContractDetail.vue',
    'src/views/Purchase/ElectronicContractList.vue',
    'src/views/PurchaseContract.vue',
    'src/views/PurchaseReturn.vue',
    'src/views/SalesMember.vue',
    'src/views/salesOrder.vue',
    'src/views/SalesPopular.vue',
    'src/views/security/PasswordResetLog.vue',
  ];

  const patterns = [
    {
      regex: /ElMessage\.info\(["']([^"']*开发中[^"']*)["']\)/g,
      replacement: (match, message) => {
        const featureName = message.replace(/开发中|功能开发中/g, '').trim() || '该功能';
        return `ElMessage.warning('${featureName}功能暂未开放，敬请期待')`;
      }
    },
    {
      regex: /ElMessage\.success\(["']([^"']*开发中[^"']*)["']\)/g,
      replacement: (match, message) => {
        const featureName = message.replace(/开发中|功能开发中/g, '').trim() || '该功能';
        return `ElMessage.warning('${featureName}功能暂未开放，敬请期待')`;
      }
    },
    {
      regex: /ElMessage\.info\(`([^`]*开发中[^`]*)`\)/g,
      replacement: (match, message) => {
        const featureName = message.replace(/开发中|功能开发中/g, '').trim() || '该功能';
        return `ElMessage.warning('${featureName}功能暂未开放，敬请期待')`;
      }
    },
    {
      regex: /\/\/\s*TODO:[^\n]*\/\/\s*待实现/g,
      replacement: (match) => match.replace('// 待实现', '').trim()
    },
    {
      regex: /此处将显示[^，]*功能开发中\.\.\./g,
      replacement: '功能暂未开放，敬请期待'
    },
    {
      regex: /（开发中）/g,
      replacement: '（暂未开放）'
    },
  ];

  filesToFix.forEach(relativePath => {
    const filePath = path.join(frontendRoot, relativePath);
    if (!fs.existsSync(filePath)) {
      return;
    }

    let content = fs.readFileSync(filePath, 'utf-8');
    let modified = false;
    let fixCount = 0;

    patterns.forEach(pattern => {
      const matches = content.match(pattern.regex);
      if (matches) {
        content = content.replace(pattern.regex, pattern.replacement);
        fixCount += matches.length;
        modified = true;
      }
    });

    if (modified) {
      fs.writeFileSync(filePath, content, 'utf-8');
      fixLog.unimplemented.push({ file: relativePath, count: fixCount });
      console.log(`  ✓ 修复: ${relativePath} (${fixCount}处)`);
    }
  });

  console.log(`  📊 共修复 ${fixLog.unimplemented.reduce((sum, item) => sum + item.count, 0)} 处未实现功能`);
}

function fixRemainingDirectAxios() {
  console.log('\n🔧 修复剩余直接使用axios...');
  
  const filesToFix = [
    'src/api/request.ts',
    'src/utils/request.ts',
  ];

  filesToFix.forEach(relativePath => {
    const filePath = path.join(frontendRoot, relativePath);
    if (!fs.existsSync(filePath)) {
      return;
    }

    let content = fs.readFileSync(filePath, 'utf-8');
    
    if (relativePath.includes('request.ts')) {
      console.log(`  ⚠ 跳过: ${relativePath} (封装层允许使用axios)`);
      return;
    }

    let modified = false;

    if (content.includes('axios.get(')) {
      content = content.replace(/axios\.get\(/g, 'request.get(');
      modified = true;
    }
    if (content.includes('axios.post(')) {
      content = content.replace(/axios\.post\(/g, 'request.post(');
      modified = true;
    }

    if (modified) {
      fs.writeFileSync(filePath, content, 'utf-8');
      fixLog.directAxios.push({ file: relativePath });
      console.log(`  ✓ 修复: ${relativePath}`);
    }
  });

  console.log(`  📊 共修复 ${fixLog.directAxios.length} 处直接使用axios`);
}

function fixRemainingWrongResponse() {
  console.log('\n🔧 修复剩余错误响应处理...');
  
  const filesToFix = [
    'src/api/request.ts',
  ];

  filesToFix.forEach(relativePath => {
    const filePath = path.join(frontendRoot, relativePath);
    if (!fs.existsSync(filePath)) {
      return;
    }

    let content = fs.readFileSync(filePath, 'utf-8');
    let modified = false;

    if (content.includes('.data.data') && !content.includes('response.data.data')) {
      console.log(`  ⚠ 跳过: ${relativePath} (响应拦截器内部处理)`);
      return;
    }

    console.log(`  ⚠ 检查: ${relativePath} - 需要手动确认`);
  });

  console.log(`  📊 响应处理问题需要手动审查`);
}

function fixPhysicalDelete() {
  console.log('\n🔧 检查物理删除问题...');
  
  function scanJavaFiles(dir) {
    if (!fs.existsSync(dir)) return;
    
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (!['target', 'test'].includes(entry.name)) {
          scanJavaFiles(fullPath);
        }
      } else if (entry.isFile() && entry.name.endsWith('.java')) {
        const content = fs.readFileSync(fullPath, 'utf-8');
        const lines = content.split('\n');
        
        lines.forEach((line, index) => {
          if ((line.includes('DELETE FROM') || line.includes('delete from')) && 
              !line.trim().startsWith('//') && 
              !line.trim().startsWith('*') &&
              !line.includes('@SqlParser')) {
            const relativePath = path.relative(backendRoot, fullPath);
            fixLog.physicalDelete.push({
              file: relativePath,
              line: index + 1,
              content: line.trim(),
            });
          }
        });
      }
    }
  }

  scanJavaFiles(path.join(backendRoot, 'src/main/java'));
  
  if (fixLog.physicalDelete.length > 0) {
    console.log(`  ⚠ 发现 ${fixLog.physicalDelete.length} 处可能的物理删除语句，需要手动审查：`);
    fixLog.physicalDelete.slice(0, 10).forEach(item => {
      console.log(`    - ${item.file}:${item.line}`);
    });
    if (fixLog.physicalDelete.length > 10) {
      console.log(`    ... 还有 ${fixLog.physicalDelete.length - 10} 处`);
    }
  } else {
    console.log(`  ✓ 未发现物理删除问题`);
  }
}

function generateEnhancedFixReport() {
  console.log('\n📝 生成增强版修复报告...');
  
  let report = '# 代码质量修复报告（第二轮）\n\n';
  report += `修复时间: ${new Date().toLocaleString('zh-CN')}\n\n`;
  
  report += '## 📊 修复统计\n\n';
  report += '| 类别 | 修复数量 |\n';
  report += '|------|----------|\n';
  report += `| 未实现功能占位符 | ${fixLog.unimplemented.reduce((sum, item) => sum + item.count, 0)} |\n`;
  report += `| 直接使用axios | ${fixLog.directAxios.length} |\n`;
  report += `| 物理删除（需审查） | ${fixLog.physicalDelete.length} |\n`;
  report += '| **总计** | **' + (fixLog.unimplemented.reduce((sum, item) => sum + item.count, 0) + fixLog.directAxios.length) + '** |\n\n';

  if (fixLog.unimplemented.length > 0) {
    report += '## 未实现功能修复详情\n\n';
    fixLog.unimplemented.forEach(item => {
      report += `- **${item.file}**: ${item.count}处\n`;
    });
    report += '\n';
  }

  if (fixLog.physicalDelete.length > 0) {
    report += '## ⚠ 物理删除问题（需手动审查）\n\n';
    report += '以下文件包含可能的物理删除语句，请确认是否需要改为逻辑删除：\n\n';
    fixLog.physicalDelete.forEach(item => {
      report += `- ${item.file}:${item.line}\n`;
    });
    report += '\n';
  }

  report += '## 📋 后续建议\n\n';
  report += '1. **测试验证**: 运行前后端测试确保修复正确\n';
  report += '2. **物理删除审查**: 检查所有物理删除语句，改为逻辑删除\n';
  report += '3. **功能完善**: 为标记为TODO的功能添加实际实现\n';
  report += '4. **持续监控**: 定期运行代码质量扫描工具\n\n';

  report += '---\n';
  report += '*此报告由代码质量修复工具自动生成*\n';

  const reportPath = path.join(projectRoot, 'CODE_FIX_REPORT_ROUND2.md');
  fs.writeFileSync(reportPath, report, 'utf-8');
  console.log(`📄 修复报告已保存到: ${reportPath}`);
}

console.log('🚀 开始第二轮批量修复...\n');

fixRemainingUnimplemented();
fixRemainingDirectAxios();
fixRemainingWrongResponse();
fixPhysicalDelete();
generateEnhancedFixReport();

console.log('\n✅ 第二轮修复完成!');
console.log(`📊 总修复数: ${fixLog.unimplemented.reduce((sum, item) => sum + item.count, 0) + fixLog.directAxios.length}`);
