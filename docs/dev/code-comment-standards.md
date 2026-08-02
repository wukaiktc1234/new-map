# 标签模板设计功能 - 代码注释规范

## 一、Java代码注释规范

### 1. 类注释
```java
/**
 * 标签模板控制器
 * 提供标签设计、预览、打印等功能
 * 
 * <p>功能说明：</p>
 * <ul>
 *   <li>模板管理：创建、更新、删除、查询模板</li>
 *   <li>预览功能：生成HTML预览和Canvas预览数据</li>
 *   <li>打印功能：单个打印、批量打印、TSPL指令生成</li>
 * </ul>
 * 
 * <p>权限说明：</p>
 * <ul>
 *   <li>查看：label:template:view</li>
 *   <li>创建：label:template:create</li>
 *   <li>更新：label:template:update</li>
 *   <li>删除：label:template:delete</li>
 *   <li>打印：label:template:print</li>
 * </ul>
 * 
 * @author 系统管理员
 * @version 1.0.0
 * @since 2026-03-06
 */
```

### 2. 方法注释
```java
/**
 * 创建标签模板
 * 
 * <p>功能说明：</p>
 * <ul>
 *   <li>验证模板名称格式</li>
 *   <li>验证标签尺寸范围</li>
 *   <li>生成唯一模板ID</li>
 *   <li>设置默认值</li>
 * </ul>
 * 
 * @param template 模板数据，包含模板名称、类型、尺寸等信息
 * @return 创建后的模板，包含生成的ID和时间戳
 * @throws IllegalArgumentException 当模板名称格式不正确时抛出
 * @throws IllegalStateException 当模板ID已存在时抛出
 */
```

### 3. 字段注释
```java
/**
 * 乐观锁版本号
 * 用于防止并发编辑冲突
 * 每次更新时自动递增
 */
@Version
private Integer version;

/**
 * 模板名称
 * 长度限制：1-50字符
 * 格式：中文、英文、数字、下划线、横线
 */
private String templateName;
```

### 4. 常量注释
```java
/**
 * 标签尺寸最小值（单位：毫米）
 */
private static final int MIN_LABEL_SIZE = 20;

/**
 * 标签尺寸最大值（单位：毫米）
 */
private static final int MAX_LABEL_SIZE = 100;

/**
 * 批量打印最大数量
 * 防止DoS攻击和资源耗尽
 */
private static final int MAX_BATCH_SIZE = 100;
```

---

## 二、TypeScript代码注释规范

### 1. 函数注释
```typescript
/**
 * HTML实体转义
 * 用于防止XSS跨站脚本攻击
 * 
 * @param text - 需要转义的文本
 * @returns 转义后的安全文本
 * 
 * @example
 * ```typescript
 * const safe = escapeHtml('<script>alert("XSS")</script>')
 * // 返回: '&lt;script&gt;alert("XSS")&lt;/script&gt;'
 * ```
 */
export function escapeHtml(text: string | null | undefined): string {
  // ...
}
```

### 2. 接口注释
```typescript
/**
 * 标签元素配置
 * 定义标签上可放置的各种元素类型
 */
export interface LabelElement {
  /**
   * 元素唯一标识
   * 格式：{type}-{timestamp}-{random}
   */
  id: string
  
  /**
   * 元素类型
   * - TEXT: 文本元素
   * - BARCODE: 条形码
   * - QRCODE: 二维码
   * - LINE: 线条
   * - BOX: 矩形框
   */
  type: LabelElementType
  
  /**
   * X坐标（单位：毫米）
   * 相对于标签左上角
   */
  x: number
}
```

### 3. 枚举注释
```typescript
/**
 * 标签元素类型枚举
 */
export enum LabelElementType {
  /** 文本元素 - 显示静态或动态文本 */
  TEXT = 'TEXT',
  
  /** 条形码元素 - 显示一维条形码 */
  BARCODE = 'BARCODE',
  
  /** 二维码元素 - 显示QR码 */
  QRCODE = 'QRCODE',
  
  /** 线条元素 - 绘制直线 */
  LINE = 'LINE',
  
  /** 矩形框元素 - 绘制矩形边框 */
  BOX = 'BOX'
}
```

---

## 三、SQL注释规范

### 1. 表注释
```sql
-- 审计日志表
-- 记录所有关键操作的日志信息
-- 用于安全审计和问题追溯
CREATE TABLE audit_log (
    -- 主键ID，自增
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 用户ID，关联用户表
    user_id VARCHAR(50) COMMENT '用户ID',
    
    -- 操作描述，如"创建标签模板"
    operation VARCHAR(200) COMMENT '操作描述',
    
    -- 创建时间，自动记录
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
```

### 2. 索引注释
```sql
-- 用户ID索引，加速按用户查询
CREATE INDEX idx_user_id ON audit_log(user_id);

-- 创建时间索引，加速按时间范围查询
CREATE INDEX idx_created_at ON audit_log(created_at);

-- 响应状态索引，加速按状态查询
CREATE INDEX idx_response_status ON audit_log(response_status);
```

---

## 四、配置文件注释规范

### 1. application.yml
```yaml
# 标签模板配置
label:
  template:
    # 标签尺寸限制
    size:
      min: 20      # 最小尺寸（毫米）
      max: 100     # 最大尺寸（毫米）
    
    # 批量打印配置
    batch:
      max-size: 100        # 单次最大打印数量
      chunk-size: 20       # 分批处理数量
      print-interval: 100  # 打印间隔（毫秒）
    
    # 限流配置
    rate-limit:
      create: 10    # 创建操作：60秒内最多10次
      print: 30     # 打印操作：60秒内最多30次
```

---

## 五、注释最佳实践

### 1. 注释原则
- **简洁明了**：用最少的文字说明最核心的内容
- **准确无误**：注释内容必须与代码逻辑一致
- **及时更新**：代码修改时同步更新注释
- **避免冗余**：不注释显而易见的代码

### 2. 注释时机
- ✅ 复杂的业务逻辑
- ✅ 非直观的算法实现
- ✅ 重要的配置项
- ✅ 公共API接口
- ✅ 关键的常量定义
- ❌ 简单的getter/setter
- ❌ 自解释的代码

### 3. 注释格式
- 使用中文注释（符合项目规范）
- 使用JavaDoc/TypeDoc标准格式
- 使用`<p>`标签分段
- 使用`<ul>`标签列表
- 使用`@param`、`@returns`、`@throws`标签

---

## 六、注释示例

### 完整示例：LabelTemplateService.java

```java
/**
 * 标签模板服务接口
 * 提供标签模板的业务逻辑处理
 * 
 * <p>主要功能：</p>
 * <ul>
 *   <li>模板CRUD操作</li>
 *   <li>模板验证</li>
 *   <li>批量打印</li>
 *   <li>默认模板管理</li>
 * </ul>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * @Autowired
 * private LabelTemplateService labelTemplateService;
 * 
 * // 创建模板
 * LabelTemplate template = new LabelTemplate();
 * template.setTemplateName("食品标签");
 * LabelTemplate created = labelTemplateService.createTemplate(template);
 * 
 * // 批量打印
 * Map<String, Object> result = labelTemplateService.batchPrintLabels(
 *     layoutConfig, 
 *     labelDataList
 * );
 * }</pre>
 * 
 * @author 系统管理员
 * @version 1.0.0
 * @since 2026-03-06
 * @see LabelTemplate
 * @see LabelTemplateServiceImpl
 */
public interface LabelTemplateService {
    
    /**
     * 创建标签模板
     * 
     * <p>处理流程：</p>
     * <ol>
     *   <li>验证模板名称格式</li>
     *   <li>验证标签尺寸范围</li>
     *   <li>生成唯一模板ID</li>
     *   <li>设置默认值</li>
     *   <li>保存到数据库</li>
     * </ol>
     * 
     * @param template 模板数据，包含以下必填字段：
     *                 <ul>
     *                   <li>templateName - 模板名称（1-50字符）</li>
     *                   <li>templateType - 模板类型</li>
     *                   <li>labelWidth - 标签宽度（20-100mm）</li>
     *                   <li>labelHeight - 标签高度（20-100mm）</li>
     *                 </ul>
     * @return 创建后的模板，包含以下新增字段：
     *         <ul>
     *           <li>id - 生成的唯一ID</li>
     *           <li>createdAt - 创建时间</li>
     *           <li>updatedAt - 更新时间</li>
     *           <li>version - 版本号（初始为0）</li>
     *         </ul>
     * @throws IllegalArgumentException 当模板名称格式不正确时抛出
     * @throws IllegalStateException 当模板ID已存在时抛出
     */
    LabelTemplate createTemplate(LabelTemplate template);
}
```

---

## 七、注释检查清单

### 提交代码前检查
- [ ] 所有公共类都有类注释
- [ ] 所有公共方法都有方法注释
- [ ] 所有参数都有@param注释
- [ ] 所有返回值都有@returns注释
- [ ] 所有异常都有@throws注释
- [ ] 注释内容与代码逻辑一致
- [ ] 注释使用中文
- [ ] 注释格式符合规范

---

**文档版本**: 1.0.0  
**最后更新**: 2026-03-06  
**维护人员**: 开发团队
