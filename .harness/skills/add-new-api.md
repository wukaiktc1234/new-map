# Skill: 新增 API 接口

> 触发条件：需要新增后端 API 接口及对应前端调用

## 完整清单（按创建顺序）

### 1. 后端 - Entity 层
```
文件: entity/XxxEntity.java
确认:
- [ ] @TableName("xxx") 注解正确
- [ ] @TableId(type = IdType.AUTO) 主键注解
- [ ] @TableLogic private Integer deleted (逻辑删除)
- [ ] 所有字段有显式 getter/setter (Delombok项目!)
- [ ] 字段命名: 驼峰式 (Java) ↔ 下划线 (DB)
```

### 2. 后端 - DTO 层
```
文件: dto/XxxCreateDTO.java, XxxUpdateDTO.java, XxxQueryDTO.java
规则:
- CreateDTO: 创建所需字段 + @Valid 校验注解
- UpdateDTO: 继承CreateDTO + id字段
- QueryDTO: 分页参数(page, size) + 查询条件
- 禁止使用 any 类型
```

### 3. 后端 - Mapper 层
```
文件: mapper/XxxMapper.java (interface)
- extends BaseMapper<XxxEntity>
- 自定义查询方法用 @Select 或 XML

文件: resources/mapper/XxxMapper.xml (如有自定义SQL)
- 确保 resultMap 与 Entity 字段匹配
- 注意 H2 兼容性 (不用 MySQL 特有语法)
```

### 4. 后端 - Service 层
```
文件: service/IXxxService.java (interface)
- 标准 CRUD: getById, getList, create, update, delete
- 分页查询: IPage<T> getPage(XxxQueryDTO dto)

文件: service/impl/XxxServiceImpl.java
- @Service 注解
- 注入 Mapper
- 业务逻辑在此实现
- 返回统一 Result 对象
```

### 5. 后端 - Controller 层
```
文件: controller/XxxController.java
- @RestController + @RequestMapping("/v1/xxx")
- @Operation(summary="") API文档注解
- 参数用 @RequestBody + @Valid 接收 DTO
- 返回 Result.success(data)
- 权限注解: @PreAuthorize("hasAuthority('xxx:read')")
```

### 6. 后端 - 安全配置
```
文件: security/config/SecurityConfig.java
- 新增白名单路径 (如果需要匿名访问)
- 或确认权限字符串与 Controller 一致
```

### 7. 前端 - Type 定义
```
文件: types/api.ts
- 定义 Request/Response 接口
- 字段与后端 DTO 对应 (camelCase)
```

### 8. 前端 - API 封装
```
文件: api/xxx.ts
- import request from './request'
- 导出 xxxApi 对象
- 方法: get/post/put/delete
- 参数: GET直接传对象, POST传data对象
- 禁止手动添加 Authorization header
```

### 9. 前端 - 路由配置
```
文件: router/index.ts
- 在合适的一级菜单下添加 children
- path 格式: /module/sub-module/action
- component: () => import("@/views/...")
- meta: { title, icon, permissions, menuType, category }
- menuType: 0=一级菜单, 1=二级分组, 2=叶子页面
```

### 10. 数据库初始化（H2环境）
```
文件: config/DatabaseInitConfig.java
- 如需新表: 添加 createXxxTable() 方法
- 在 initDevDatabase() 中调用
- 确保 CREATE TABLE IF NOT EXISTS
- 包含 deleted TINYINT DEFAULT 0 字段
```

## 验证清单
```bash
# 1. 后端编译
cd backend && mvn compile -q

# 2. 启动后端并测试 API
curl -s http://localhost:8081/api/v1/xxx/list | python -m json.tool

# 3. 前端编译
cd frontend && npm run build

# 4. 功能测试: 浏览器访问新页面
```

## 注意事项
- 本项目使用 request 实例发送请求，禁止直接用 axios
- 响应拦截器已提取 .data.data，前端不访问 response.data
- 所有表必须有 deleted 字段（逻辑删除）
- 禁止物理删除（DELETE SQL），使用 removeById()
