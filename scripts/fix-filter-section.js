/**
 * 批量修复脚本：将 fts-filter-section 从 ContentCard 外部移入内部
 * 
 * 使用方法: node scripts/fix-filter-section.js
 */

const fs = require('fs');
const path = require('path');

const VIEWS_DIR = path.join(__dirname, '../frontend/src/views');

// 统计信息
let stats = {
  total: 0,
  fixed: 0,
  skipped: 0,
  errors: []
};

/**
 * 检查文件是否需要修复
 * 模式: fts-filter-section 出现在 ContentCard 开始标签之前
 */
function needsFix(content) {
  // 查找 fts-filter-section 的位置
  const filterSectionStart = content.indexOf('<div class="fts-filter-section">');
  if (filterSectionStart === -1) return { needsFix: false };
  
  // 查找第一个 ContentCard 的位置（在 filterSection 之后）
  const contentCardAfterFilter = content.indexOf('<ContentCard', filterSectionStart);
  
  // 查找 filter-section 的结束标签
  const filterSectionEnd = content.indexOf('</div>', content.indexOf('</div>', filterSectionStart) + 6);
  
  // 如果 ContentCard 在 filter-section 之后才出现，说明 filter-section 在外面
  if (contentCardAfterFilter !== -1 && contentCardAfterFilter > filterSectionStart) {
    // 检查 filter-section 是否已经在一个 ContentCard 内部
    // 简单检查：看 filter-section 前面是否有未关闭的 ContentCard
    const beforeFilter = content.substring(0, filterSectionStart);
    const openContentCards = (beforeFilter.match(/<ContentCard/g) || []).length;
    const closeContentCards = (beforeFilter.match(/<\/ContentCard>/g) || []).length;
    
    if (openContentCards <= closeContentCards) {
      return { 
        needsFix: true, 
        filterSectionStart,
        filterSectionEnd: filterSectionEnd + 6 // 包含 </div>
      };
    }
  }
  
  return { needsFix: false };
}

/**
 * 提取 fts-filter-section 块
 */
function extractFilterSection(content, start) {
  let depth = 0;
  let i = start;
  let inTag = false;
  
  while (i < content.length) {
    if (content[i] === '<' && content[i+1] !== '/') {
      inTag = true;
      if (content.substring(i, i + 27) === '<div class="fts-filter-section" || 
          (depth > 0 && content.substring(i, i + 4) === '<div')) {
        depth++;
      }
    } else if (content[i] === '>' && inTag) {
      inTag = false;
    } else if (content[i] === '<' && content[i+1] === '/' && content.substring(i, i+6) === '</div>') {
      depth--;
      if (depth === 0) {
        // 找到匹配的结束标签
        const endDiv = content.indexOf('>', i);
        return {
          start: start,
          end: endDiv + 1,
          content: content.substring(start, endDiv + 1)
        };
      }
    }
    i++;
  }
  
  return null;
}

/**
 * 修复单个文件
 */
function fixFile(filePath) {
  try {
    let content = fs.readFileSync(filePath, 'utf-8');
    stats.total++;
    
    const checkResult = needsFix(content);
    if (!checkResult.needsFix) {
      stats.skipped++;
      return null;
    }
    
    // 提取完整的 fts-filter-section 块
    const filterBlock = extractFilterSection(content, checkResult.filterSectionStart);
    if (!filterBlock) {
      stats.skipped++;
      return null;
    }
    
    // 查找目标 ContentCard（filter-section 后面的第一个）
    const afterFilter = content.substring(filterBlock.end);
    const contentCardMatch = afterFilter.match(/<ContentCard[\s>]/);
    if (!contentCardMatch) {
      stats.skipped++;
      return null;
    }
    
    const contentCardPos = filterBlock.end + contentCardMatch.index;
    
    // 查找 ContentCard 的 #actions 或 #header 结束位置，或直接在 <ContentCard> 后插入
    // 策略：找到 </template #actions> 或如果没有 actions，找到 <template #header> 后的下一个标签
    
    let insertPos = contentCardPos;
    const afterContentCard = content.substring(contentCardPos, contentCardPos + 500);
    
    // 尝试找到 #actions 结束位置
    const actionsEndMatch = afterContentCard.match(/<\/template>\s*\n\s*(?=<)/);
    const headerEndMatch = afterContentCard.match(<\/template>\s*\n\s*(?=<)/);
    
    if (actionsEndMatch) {
      insertPos = contentCardPos + actionsEndMatch.index + actionsEndMatch[0].length;
    } else if (headerEndMatch) {
      insertPos = contentCardPos + headerEndMatch.index + headerEndMatch[0].length;
    } else {
      // 如果没有 template slot，尝试找到 ContentCard 标签结束后的位置
      const tagEndMatch = afterContentCard.match(/<ContentCard[^>]*>/);
      if (tagEndMatch) {
        insertPos = contentCardPos + tagEndMatch.index + tagEndMatch[0].length;
      }
    }
    
    // 构建新内容
    const beforeFilter = content.substring(0, filterBlock.start);
    const betweenFilterAndInsert = content.substring(filterBlock.end, insertPos);
    const afterInsert = content.substring(insertPos);
    
    // 清理 beforeFilter 中可能的多余空行
    const cleanedBefore = beforeFilter.replace(/\n\s*\n\s*\n/g, '\n\n');
    
    // 新内容：移除 filter-section，并在 insertPos 处添加
    const newContent = cleanedBefore + 
                      '\n' +
                      betweenFilterAndInsert + 
                      '\n      ' + 
                      filterBlock.content.trim().split('\n').join('\n      ') + 
                      '\n' +
                      afterInsert;
    
    fs.writeFileSync(filePath, newContent, 'utf-8');
    stats.fixed++;
    
    return path.relative(VIEWS_DIR, filePath);
  } catch (error) {
    stats.errors.push({ file: filePath, error: error.message });
    return null;
  }
}

/**
 * 递归遍历目录
 */
function walkDir(dir, callback) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    if (stat.isDirectory()) {
      walkDir(filePath, callback);
    } else if (file.endsWith('.vue')) {
      callback(filePath);
    }
  }
}

// 主函数
console.log('开始批量修复 fts-filter-section 位置...\n');

walkDir(VIEWS_DIR, (filePath) => {
  const result = fixFile(filePath);
  if (result) {
    console.log(`[已修复] ${result}`);
  }
});

console.log('\n========== 修复完成 ==========');
console.log(`总文件数: ${stats.total}`);
console.log(`已修复: ${stats.fixed}`);
console.log(`跳过: ${stats.skipped}`);
if (stats.errors.length > 0) {
  console.log('\n错误:');
  stats.errors.forEach(e => console.log(`  - ${e.file}: ${e.error}`));
}
