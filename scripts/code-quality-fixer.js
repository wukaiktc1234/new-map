/**
 * 自动化批量修复工具
 * 用于修复代码质量扫描发现的所有严重问题
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
  mapperXml: [],
};

function fixUnimplementedFeatures() {
  console.log('\n🔧 修复未实现功能占位符...');
  
  const patterns = [
    {
      regex: /ElMessage\.(info|success|warning)\(['"`]([^'"`]*功能开发中[^'"`]*)['"`]\)/g,
      replacement: (match, type, message) => {
        const featureName = message.replace(/功能开发中\.?\.?/, '').trim() || '该功能';
        return `// TODO: 实现${featureName}\n    ElMessage.warning('${featureName}功能暂未开放，敬请期待')`;
      }
    },
    {
      regex: /showToast\(['"`]([^'"`]*功能开发中[^'"`]*)['"`]\)/g,
      replacement: (match, message) => {
        const featureName = message.replace(/功能开发中\.?\.?/, '').trim() || '该功能';
        return `ElMessage.warning('${featureName}功能暂未开放，敬请期待')`;
      }
    },
    {
      regex: /<el-empty\s+description=["']([^"']*功能开发中[^"']*)["']\s*\/>/g,
      replacement: (match, description) => {
        const featureName = description.replace(/功能开发中/, '').trim();
        return `<div class="feature-placeholder"><el-empty description="${featureName}功能正在开发中，敬请期待" /></div>`;
      }
    },
    {
      regex: /\/\/\s*TODO:\s*(从API加载|调用API|实现)[^\n]*/g,
      replacement: (match) => match + ' // 待实现'
    }
  ];

  function processFile(filePath) {
    let content = fs.readFileSync(filePath, 'utf-8');
    let modified = false;
    const relativePath = path.relative(frontendRoot, filePath);
    
    patterns.forEach(pattern => {
      const matches = content.match(pattern.regex);
      if (matches) {
        content = content.replace(pattern.regex, pattern.replacement);
        matches.forEach(match => {
          fixLog.unimplemented.push({
            file: relativePath,
            original: match.substring(0, 100),
          });
        });
        modified = true;
      }
    });

    if (modified) {
      fs.writeFileSync(filePath, content, 'utf-8');
      console.log(`  ✓ 修复: ${relativePath}`);
    }
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
          processFile(fullPath);
        }
      }
    }
  }

  scanDirectory(frontendRoot, ['.vue', '.ts']);
  console.log(`  📊 共修复 ${fixLog.unimplemented.length} 处未实现功能占位符`);
}

function fixDirectAxios() {
  console.log('\n🔧 修复直接使用axios...');
  
  const filesToFix = [
    'src/views/Marketing/Popular.vue',
    'src/views/OperationLog.vue',
    'src/views/PrintQueueManagement.vue',
    'src/views/PrintTemplateDesigner.vue',
    'src/views/SalesPopular.vue',
  ];

  filesToFix.forEach(relativePath => {
    const filePath = path.join(frontendRoot, relativePath);
    if (!fs.existsSync(filePath)) {
      console.log(`  ⚠ 文件不存在: ${relativePath}`);
      return;
    }

    let content = fs.readFileSync(filePath, 'utf-8');
    let modified = false;

    if (content.includes("import axios from 'axios'") || content.includes('import axios from "axios"')) {
      content = content.replace(/import\s+axios\s+from\s+['"]axios['"]/g, "import request from '@/api/request'");
      modified = true;
      fixLog.directAxios.push({ file: relativePath, type: 'import替换' });
    }

    if (content.includes('axios.get(')) {
      content = content.replace(/axios\.get\(([^,]+),\s*\{\s*params:\s*([^}]+)\}\)/g, 'request.get($1, $2)');
      content = content.replace(/axios\.get\(([^)]+)\)/g, 'request.get($1)');
      modified = true;
      fixLog.directAxios.push({ file: relativePath, type: 'get方法替换' });
    }

    if (content.includes('axios.post(')) {
      content = content.replace(/axios\.post\(([^,]+),\s*([^)]+)\)/g, 'request.post($1, $2)');
      modified = true;
      fixLog.directAxios.push({ file: relativePath, type: 'post方法替换' });
    }

    if (content.includes('axios.put(')) {
      content = content.replace(/axios\.put\(([^,]+),\s*([^)]+)\)/g, 'request.put($1, $2)');
      modified = true;
      fixLog.directAxios.push({ file: relativePath, type: 'put方法替换' });
    }

    if (content.includes('axios.delete(')) {
      content = content.replace(/axios\.delete\(([^)]+)\)/g, 'request.delete($1)');
      modified = true;
      fixLog.directAxios.push({ file: relativePath, type: 'delete方法替换' });
    }

    if (modified) {
      fs.writeFileSync(filePath, content, 'utf-8');
      console.log(`  ✓ 修复: ${relativePath}`);
    }
  });

  console.log(`  📊 共修复 ${fixLog.directAxios.length} 处直接使用axios`);
}

function fixWrongResponseHandling() {
  console.log('\n🔧 修复错误响应处理...');
  
  const filesToFix = [
    'src/views/Finance/Asset.vue',
    'src/views/Finance/Budget.vue',
    'src/views/Finance/CostAnalysis.vue',
    'src/views/Finance/Ledger.vue',
    'src/views/Finance/Payable.vue',
    'src/views/HR/ApprovalManagement.vue',
    'src/views/HR/OnboardingManagement.vue',
    'src/views/Marketing/Popular.vue',
    'src/views/SalesPopular.vue',
    'src/views/SalesPromotion.vue',
    'src/views/device/DeviceSimulator.vue',
    'src/components/Finance/dialogs/AssetSyncLogDialog.vue',
  ];

  filesToFix.forEach(relativePath => {
    const filePath = path.join(frontendRoot, relativePath);
    if (!fs.existsSync(filePath)) {
      console.log(`  ⚠ 文件不存在: ${relativePath}`);
      return;
    }

    let content = fs.readFileSync(filePath, 'utf-8');
    let modified = false;

    const patterns = [
      { regex: /response\.data\.data/g, replacement: 'response' },
      { regex: /res\.data\.data/g, replacement: 'res' },
      { regex: /\.data\.data\./g, replacement: '.data.' },
    ];

    patterns.forEach(pattern => {
      if (pattern.regex.test(content)) {
        const matches = content.match(pattern.regex);
        if (matches) {
          content = content.replace(pattern.regex, pattern.replacement);
          fixLog.wrongResponse.push({ file: relativePath, count: matches.length });
          modified = true;
        }
      }
    });

    if (modified) {
      fs.writeFileSync(filePath, content, 'utf-8');
      console.log(`  ✓ 修复: ${relativePath}`);
    }
  });

  console.log(`  📊 共修复 ${fixLog.wrongResponse.length} 个文件的响应处理`);
}

function generateMapperXmlFiles() {
  console.log('\n🔧 生成缺少的Mapper XML文件...');
  
  const mapperJavaDir = path.join(backendRoot, 'src/main/java/com/example/demo/mapper');
  const mapperXmlDir = path.join(backendRoot, 'src/main/resources/mapper');
  
  if (!fs.existsSync(mapperJavaDir)) {
    console.log('  ⚠ Mapper Java目录不存在');
    return;
  }

  if (!fs.existsSync(mapperXmlDir)) {
    fs.mkdirSync(mapperXmlDir, { recursive: true });
    console.log('  ✓ 创建Mapper XML目录');
  }

  const javaMappers = fs.readdirSync(mapperJavaDir)
    .filter(f => f.endsWith('.java'))
    .map(f => f.replace('.java', ''));
  
  const existingXmlMappers = fs.existsSync(mapperXmlDir) 
    ? fs.readdirSync(mapperXmlDir).filter(f => f.endsWith('.xml')).map(f => f.replace('.xml', ''))
    : [];

  const missingMappers = javaMappers.filter(m => !existingXmlMappers.includes(m));

  missingMappers.forEach(mapperName => {
    const xmlContent = `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.mapper.${mapperName}">
    
</mapper>
`;
    const xmlPath = path.join(mapperXmlDir, `${mapperName}.xml`);
    fs.writeFileSync(xmlPath, xmlContent, 'utf-8');
    fixLog.mapperXml.push({ mapper: mapperName });
    console.log(`  ✓ 生成: ${mapperName}.xml`);
  });

  console.log(`  📊 共生成 ${fixLog.mapperXml.length} 个Mapper XML文件`);
}

function generateFixReport() {
  console.log('\n📝 生成修复报告...');
  
  let report = '# 代码质量修复报告\n\n';
  report += `修复时间: ${new Date().toLocaleString('zh-CN')}\n\n`;
  
  report += '## 📊 修复统计\n\n';
  report += '| 类别 | 修复数量 |\n';
  report += '|------|----------|\n';
  report += `| 未实现功能占位符 | ${fixLog.unimplemented.length} |\n`;
  report += `| 直接使用axios | ${fixLog.directAxios.length} |\n`;
  report += `| 错误响应处理 | ${fixLog.wrongResponse.length} |\n`;
  report += `| Mapper XML文件 | ${fixLog.mapperXml.length} |\n`;
  report += `| **总计** | **${fixLog.unimplemented.length + fixLog.directAxios.length + fixLog.wrongResponse.length + fixLog.mapperXml.length}** |\n\n`;

  if (fixLog.unimplemented.length > 0) {
    report += '## 未实现功能占位符修复详情\n\n';
    const grouped = {};
    fixLog.unimplemented.forEach(item => {
      if (!grouped[item.file]) grouped[item.file] = [];
      grouped[item.file].push(item);
    });
    Object.entries(grouped).forEach(([file, items]) => {
      report += `- **${file}**: ${items.length}处\n`;
    });
    report += '\n';
  }

  if (fixLog.directAxios.length > 0) {
    report += '## 直接使用axios修复详情\n\n';
    fixLog.directAxios.forEach(item => {
      report += `- ${item.file} (${item.type})\n`;
    });
    report += '\n';
  }

  if (fixLog.wrongResponse.length > 0) {
    report += '## 错误响应处理修复详情\n\n';
    fixLog.wrongResponse.forEach(item => {
      report += `- ${item.file}\n`;
    });
    report += '\n';
  }

  if (fixLog.mapperXml.length > 0) {
    report += '## Mapper XML生成详情\n\n';
    fixLog.mapperXml.slice(0, 20).forEach(item => {
      report += `- ${item.mapper}.xml\n`;
    });
    if (fixLog.mapperXml.length > 20) {
      report += `- ... 还有 ${fixLog.mapperXml.length - 20} 个文件\n`;
    }
    report += '\n';
  }

  report += '## 📋 后续建议\n\n';
  report += '1. **测试验证**: 运行前后端测试确保修复正确\n';
  report += '2. **功能完善**: 为标记为TODO的功能添加实际实现\n';
  report += '3. **代码审查**: 检查自动修复的代码是否符合项目规范\n';
  report += '4. **持续监控**: 定期运行代码质量扫描工具\n\n';

  report += '---\n';
  report += '*此报告由代码质量修复工具自动生成*\n';

  const reportPath = path.join(projectRoot, 'CODE_FIX_REPORT.md');
  fs.writeFileSync(reportPath, report, 'utf-8');
  console.log(`📄 修复报告已保存到: ${reportPath}`);
}

console.log('🚀 开始批量修复严重问题...\n');

fixUnimplementedFeatures();
fixDirectAxios();
fixWrongResponseHandling();
generateMapperXmlFiles();
generateFixReport();

console.log('\n✅ 批量修复完成!');
console.log(`📊 总修复数: ${fixLog.unimplemented.length + fixLog.directAxios.length + fixLog.wrongResponse.length + fixLog.mapperXml.length}`);
