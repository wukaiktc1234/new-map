# API接口设计规范

## 一、API设计原则

### 1.1 RESTful风格

- 资源以名词表示，操作以HTTP方法区分
- 使用标准HTTP方法：GET（查询）、POST（创建）、PUT（更新）、DELETE（删除）
- URL中只包含资源名称，不包含动词
- 复数形式表示资源集合，如`/users`、`/foods`

### 1.2 统一前缀

所有API路径遵循统一前缀格式：

```
/api/v1/{module}/{resource}
```

示例：

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | /api/v1/auth | 登录、注册、令牌管理 |
| 用户 | /api/v1/users | 用户管理 |
| 菜品 | /api/v1/foods | 菜品管理 |
| 订单 | /api/v1/orders | 订单管理 |

### 1.3 版本控制

- 采用URL路径版本控制方式，当前版本为`v1`
- 版本号位于路径中，如`/api/v1/`、`/api/v2/`
- 版本升级时保持向后兼容，旧版本至少保留一个迭代周期
- 重大变更（破坏性变更）必须升级版本号

### 1.4 幂等性保障

| HTTP方法 | 幂等性 | 说明 |
|---------|--------|------|
| GET | 幂等 | 多次请求结果一致 |
| PUT | 幂等 | 全量更新，多次请求结果一致 |
| DELETE | 幂等 | 删除同一资源，多次请求结果一致 |
| POST | 非幂等 | 创建资源，多次请求可能产生多条记录 |

幂等性保障措施：

- PUT请求使用全量更新，避免增量更新导致的不一致
- DELETE请求返回统一结果，重复删除不报错
- POST创建请求可携带唯一业务标识，防止重复创建
- 关键操作使用乐观锁或唯一约束保障数据一致性

---

## 二、统一响应格式

**重要说明：后端Result类中`code=0`表示成功，非0表示失败。这与HTTP状态码无关，`code`是业务状态码。**

### 2.1 成功响应

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {},
  "timestamp": 1712345678901
}
```

### 2.2 失败响应

```json
{
  "code": 1001,
  "message": "菜品名称已存在",
  "data": null,
  "timestamp": 1712345678901
}
```

### 2.3 分页响应

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "records": [],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  },
  "timestamp": 1712345678901
}
```

### 2.4 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 业务状态码，0表示成功，非0表示失败 |
| message | String | 响应消息，成功时为"操作成功"，失败时为具体错误信息 |
| data | T | 响应数据，泛型类型，失败时为null |
| timestamp | Long | 服务器时间戳（毫秒） |

---

## 三、HTTP状态码规范

### 3.1 状态码定义

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 请求处理成功，GET查询、PUT更新、DELETE删除 |
| 201 | 已创建 | POST创建资源成功 |
| 400 | 请求错误 | 请求参数格式错误、缺少必填参数 |
| 401 | 未授权 | 未登录或Token过期 |
| 403 | 禁止访问 | 无权限访问该资源 |
| 404 | 未找到 | 请求的资源不存在 |
| 422 | 数据验证失败 | 业务规则校验不通过 |
| 500 | 服务器错误 | 服务器内部异常 |

### 3.2 使用规则

- HTTP状态码表示请求的传输层状态，业务状态通过响应体中的`code`字段表示
- 即使HTTP状态码为200，业务`code`也可能非0，表示业务处理失败
- 4xx错误不应触发服务端告警，5xx错误应触发告警
- 401错误前端应自动跳转登录页

---

## 四、后端Controller规范

### 4.1 基本规范

```java
@RestController
@RequestMapping("/api/v1/foods")
@Tag(name = "菜品管理", description = "菜品相关接口")
public class FoodController {

    @Autowired
    private FoodService foodService;

    // 接口方法
}
```

### 4.2 接口注解规范

```java
@Operation(summary = "获取菜品列表", description = "分页查询菜品列表")
@GetMapping
public Result<PageResponse<FoodVO>> getFoodList(@Valid FoodQueryDTO queryDTO) {
    return Result.success(foodService.getFoodList(queryDTO));
}

@Operation(summary = "创建菜品", description = "新增菜品信息")
@PostMapping
public Result<FoodVO> createFood(@Valid @RequestBody FoodCreateDTO createDTO) {
    return Result.success(foodService.createFood(createDTO));
}

@Operation(summary = "更新菜品", description = "根据ID更新菜品信息")
@PutMapping("/{id}")
public Result<FoodVO> updateFood(
        @PathVariable String id,
        @Valid @RequestBody FoodUpdateDTO updateDTO) {
    return Result.success(foodService.updateFood(id, updateDTO));
}

@Operation(summary = "删除菜品", description = "根据ID逻辑删除菜品")
@DeleteMapping("/{id}")
public Result<Void> deleteFood(@PathVariable String id) {
    foodService.deleteFood(id);
    return Result.success();
}
```

### 4.3 DTO命名规范

| DTO类型 | 命名格式 | 用途 | 示例 |
|---------|---------|------|------|
| 创建DTO | {Entity}CreateDTO | 接收创建请求参数 | FoodCreateDTO |
| 更新DTO | {Entity}UpdateDTO | 接收更新请求参数 | FoodUpdateDTO |
| 查询DTO | {Entity}QueryDTO | 接收查询过滤参数 | FoodQueryDTO |
| 基础信息DTO | {Entity}BasicInfo | 缓存基础数据传输 | FoodBasicInfo |
| 视图对象 | {Entity}VO | 返回给前端的视图数据 | FoodVO |

### 4.4 参数验证

```java
public class FoodCreateDTO {

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 100, message = "菜品名称不能超过100个字符")
    private String name;

    @NotNull(message = "菜品分类不能为空")
    private String categoryId;

    @NotNull(message = "菜品价格不能为空")
    @DecimalMin(value = "0.01", message = "菜品价格必须大于0")
    private BigDecimal price;

    @Size(max = 500, message = "菜品描述不能超过500个字符")
    private String description;
}
```

### 4.5 禁止事项

- 禁止在Controller中直接注入Mapper，必须通过Service层访问数据
- 禁止在Controller中编写业务逻辑，仅负责参数接收和结果返回
- 禁止直接返回实体类，必须使用VO或DTO
- 禁止在Controller中处理事务

---

## 五、后端Service规范

### 5.1 接口与实现分离

```java
// Service接口
public interface FoodService {
    PageResponse<FoodVO> getFoodList(FoodQueryDTO queryDTO);
    FoodVO createFood(FoodCreateDTO createDTO);
    FoodVO updateFood(String id, FoodUpdateDTO updateDTO);
    void deleteFood(String id);
}

// Service实现
@Service
public class FoodServiceImpl implements FoodService {
    // 实现方法
}
```

### 5.2 DataService接口（带缓存）

DataService提供带缓存的基础数据查询能力，适用于需要频繁查询且变更较少的基础数据。

```java
public interface FoodDataService {

    /**
     * 批量获取菜品基础信息
     * @param foodIds 菜品ID列表
     * @return 菜品基础信息Map，key为菜品ID
     */
    Map<String, FoodBasicInfo> batchGetFoodBasicInfo(List<String> foodIds);

    /**
     * 获取单个菜品基础信息
     * @param foodId 菜品ID
     * @return 菜品基础信息
     */
    FoodBasicInfo getFoodBasicInfo(String foodId);

    /**
     * 清除指定菜品缓存
     * @param foodId 菜品ID
     */
    void clearFoodCache(String foodId);

    /**
     * 批量清除菜品缓存
     * @param foodIds 菜品ID列表
     */
    void clearFoodBatchCache(List<String> foodIds);

    /**
     * 清除所有菜品缓存
     */
    void clearAllFoodCache();
}
```

### 5.3 DataService实现要点

- 查询时先查缓存，缓存未命中再查数据库并回填缓存
- 缓存键格式：`{entity}:basic:{entityId}`，如`food:basic:1234567890`
- 数据更新或删除时清除对应缓存
- 批量操作时批量清除缓存
- 缓存过期时间参考项目缓存规范

---

## 六、前端API规范（强制）

### 6.1 请求实例使用

- **必须**使用`src/api/request.ts`导出的`request`实例
- **禁止**直接使用`axios`或`fetch`发送请求
- **禁止**创建新的axios实例

### 6.2 参数传递规范

```typescript
// 正确写法
request.get(url, params)        // GET请求：直接传params对象
request.post(url, data)         // POST请求：直接传data对象
request.put(url, data)          // PUT请求：直接传data对象
request.delete(url, params)     // DELETE请求：直接传params对象

// 错误写法（禁止）
request.get(url, { params })    // 禁止嵌套params
axios.get(url)                  // 禁止直接使用axios
fetch(url)                      // 禁止使用fetch
```

### 6.3 Token处理

- Token由请求拦截器自动添加，**禁止**手动添加Authorization header
- Token存储在localStorage，key为`token`
- Token过期时自动跳转登录页

### 6.4 响应数据处理

响应拦截器已自动提取`response.data.data`，前端直接使用返回值：

```typescript
// 正确写法
const response = await api.getList({ page: 1, size: 10 });
const records = response?.records || [];
const total = response?.total || 0;

// 错误写法（禁止）
const response = await api.getList({ page: 1, size: 10 });
const records = response.data?.records || [];  // 禁止再访问.data
```

---

## 七、前端API文件结构规范

### 7.1 文件组织

API文件按业务模块组织在`src/api/`目录下，每个模块一个文件：

```
src/api/
├── request.ts          # 请求实例（统一入口）
├── auth.ts             # 认证相关API
├── user.ts             # 用户管理API
├── food.ts             # 菜品管理API
├── order.ts            # 订单管理API
├── inventory.ts        # 库存管理API
├── purchase.ts         # 采购管理API
├── employee.ts         # 员工管理API
└── device.ts           # 设备管理API
```

### 7.2 文件模板

```typescript
import request from './request';

// 响应类型定义
interface ExampleResponse {
  records: ExampleItem[];
  total: number;
}

// API对象导出
export const exampleApi = {
  /**
   * 获取列表
   * @param params - 查询参数
   */
  getList(params: { page: number; size: number }): Promise<ExampleResponse> {
    return request.get<ExampleResponse>('/v1/examples', params);
  },

  /**
   * 根据ID获取详情
   * @param id - 资源ID
   */
  getById(id: string): Promise<ExampleItem> {
    return request.get<ExampleItem>(`/v1/examples/${id}`);
  },

  /**
   * 创建资源
   * @param data - 创建数据
   */
  create(data: ExampleCreateDTO): Promise<ExampleItem> {
    return request.post<ExampleItem>('/v1/examples', data);
  },

  /**
   * 更新资源
   * @param id - 资源ID
   * @param data - 更新数据
   */
  update(id: string, data: ExampleUpdateDTO): Promise<ExampleItem> {
    return request.put<ExampleItem>(`/v1/examples/${id}`, data);
  },

  /**
   * 删除资源
   * @param id - 资源ID
   */
  delete(id: string): Promise<void> {
    return request.delete(`/v1/examples/${id}`);
  }
};
```

### 7.3 类型定义规范

- 类型定义集中在`src/types/`目录下管理
- 禁止使用`any`类型，必须定义明确类型
- 优先使用`interface`而非`type`
- 表单类型命名：`{Entity}FormData`、`{Entity}QueryForm`
- 响应类型命名：`{Entity}Response`、`{Entity}ListResponse`

---

## 八、分页数据格式

### 8.1 分页请求参数

```typescript
interface PageQuery {
  current: number;    // 当前页码，从1开始
  size: number;       // 每页条数，默认10
}
```

### 8.2 分页响应格式

```typescript
interface PageResponse<T> {
  records: T[];       // 数据列表
  total: number;      // 总记录数
  current: number;    // 当前页码
  size: number;       // 每页条数
  pages: number;      // 总页数
}
```

### 8.3 分页查询示例

```typescript
// 前端调用
const response = await foodApi.getList({ current: 1, size: 10 });
const records = response?.records || [];
const total = response?.total || 0;
```

---

## 九、错误处理规范

### 9.1 后端错误处理

使用`@RestControllerAdvice`全局异常处理器，返回统一错误响应：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.fail(ErrorCode.VALIDATION_ERROR, message);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }
}
```

### 9.2 前端错误处理

使用`try/catch`配合类型守卫处理错误：

```typescript
try {
  const response = await api.getData();
  // 处理数据
} catch (error: unknown) {
  // 使用类型守卫处理错误
  if (error instanceof Error) {
    ElMessage.error(error.message || '操作失败');
  }
}
```

### 9.3 错误码枚举

错误码按模块分类，格式为`{模块前缀}{序号}`：

| 模块 | 错误码范围 | 说明 |
|------|-----------|------|
| 产品 | 1000-1999 | 菜品、产品相关错误 |
| 分类 | 2000-2999 | 分类管理相关错误 |
| 溯源 | 3000-3999 | 食品溯源相关错误 |
| 认证 | 4000-4999 | 认证授权相关错误 |
| 用户 | 5000-5999 | 用户管理相关错误 |
| 设备 | 6000-6999 | 设备管理相关错误 |
| 操作 | 7000-7999 | 操作日志相关错误 |
| 采购 | 8000-8999 | 采购管理相关错误 |
| 凭证 | 9000-9999 | 凭证管理相关错误 |

常用错误码示例：

| 错误码 | 含义 | 说明 |
|--------|------|------|
| 0 | 成功 | 请求处理成功 |
| 400 | 请求参数错误 | 参数格式或内容不合法 |
| 401 | 未授权 | 未登录或Token过期 |
| 403 | 禁止访问 | 无权限访问该资源 |
| 1001 | 菜品名称已存在 | 创建菜品时名称重复 |
| 1002 | 菜品不存在 | 查询的菜品ID不存在 |
| 4001 | 用户名或密码错误 | 登录认证失败 |
| 4002 | Token已过期 | 需要刷新令牌 |
| 5001 | 用户已存在 | 注册时用户名重复 |
| 6001 | 设备已绑定 | 设备已被其他门店绑定 |

---

## 十、数据转换规则

### 10.1 性别字段

| 前端值 | 后端值 | 数据库值 |
|--------|--------|----------|
| male | male | male |
| female | female | female |
| other | other | other |

前后端保持一致，使用字符串枚举。

### 10.2 状态字段

| 业务含义 | 前端值 | 后端值 | 数据库值 |
|---------|--------|--------|----------|
| 启用/正常 | active | 1 | 1 |
| 禁用/停用 | inactive | 0 | 0 |
| 试用/审核 | probation | 2 | 2 |

前端使用语义化字符串，后端和数据库使用数字编码。前端在提交时需进行转换。

### 10.3 日期时间

| 场景 | 格式 | 说明 |
|------|------|------|
| 前端显示 | YYYY-MM-DD HH:mm:ss | 用户可读格式 |
| 后端存储 | LocalDateTime | Java时间类型 |
| API传输 | ISO 8601 | 如2024-04-01T12:00:00 |
| 数据库存储 | TIMESTAMP | PostgreSQL时间戳 |

### 10.4 部门/职位

| 场景 | 格式 | 说明 |
|------|------|------|
| 前端显示 | 名称 | 如"财务部"、"厨师长" |
| 前端提交 | ID | 提交时传部门/职位ID |
| 后端存储 | ID | 存储关联ID |
| 后端返回 | ID+名称 | 同时返回ID和名称 |

### 10.5 金额

| 场景 | 格式 | 说明 |
|------|------|------|
| 前端显示 | 元 | 如"12.50" |
| 前端输入 | 元 | 用户输入以元为单位 |
| 前端提交 | 分 | 提交时转换为分，如1250 |
| 后端存储 | 分 | 以整数为单位存储，避免浮点精度问题 |
| 数据库存储 | BIGINT | 以分为单位存储 |

转换公式：

- 元转分：`Math.round(yuan * 100)`
- 分转元：`(fen / 100).toFixed(2)`

---

## 十一、API模块清单

### 11.1 认证模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/auth/login | 用户登录 |
| POST | /api/v1/auth/register | 用户注册 |
| POST | /api/v1/auth/captcha | 获取验证码 |
| PUT | /api/v1/auth/password | 修改密码 |
| POST | /api/v1/auth/refresh | 刷新令牌 |
| POST | /api/v1/auth/logout | 退出登录 |

### 11.2 用户模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/users | 获取用户列表 |
| GET | /api/v1/users/{id} | 获取用户详情 |
| POST | /api/v1/users | 创建用户 |
| PUT | /api/v1/users/{id} | 更新用户 |
| DELETE | /api/v1/users/{id} | 删除用户 |
| PUT | /api/v1/users/{id}/status | 更新用户状态 |
| PUT | /api/v1/users/{id}/password | 重置用户密码 |

### 11.3 角色模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/roles | 获取角色列表 |
| GET | /api/v1/roles/{id} | 获取角色详情 |
| POST | /api/v1/roles | 创建角色 |
| PUT | /api/v1/roles/{id} | 更新角色 |
| DELETE | /api/v1/roles/{id} | 删除角色 |
| GET | /api/v1/roles/{id}/permissions | 获取角色权限 |
| PUT | /api/v1/roles/{id}/permissions | 分配角色权限 |

### 11.4 权限模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/permissions | 获取权限列表 |
| GET | /api/v1/permissions/tree | 获取权限树 |
| POST | /api/v1/permissions | 创建权限 |
| PUT | /api/v1/permissions/{id} | 更新权限 |
| DELETE | /api/v1/permissions/{id} | 删除权限 |

### 11.5 菜品模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/foods | 获取菜品列表 |
| GET | /api/v1/foods/{id} | 获取菜品详情 |
| POST | /api/v1/foods | 创建菜品 |
| PUT | /api/v1/foods/{id} | 更新菜品 |
| DELETE | /api/v1/foods/{id} | 删除菜品 |
| PUT | /api/v1/foods/{id}/status | 更新菜品状态 |
| POST | /api/v1/foods/batch | 批量创建菜品 |
| DELETE | /api/v1/foods/batch | 批量删除菜品 |

### 11.6 订单模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/orders | 获取订单列表 |
| GET | /api/v1/orders/{id} | 获取订单详情 |
| POST | /api/v1/orders | 创建订单 |
| PUT | /api/v1/orders/{id} | 更新订单 |
| PUT | /api/v1/orders/{id}/status | 更新订单状态 |
| POST | /api/v1/orders/{id}/cancel | 取消订单 |
| GET | /api/v1/orders/statistics | 订单统计 |

### 11.7 库存模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/inventory | 获取库存列表 |
| GET | /api/v1/inventory/{id} | 获取库存详情 |
| PUT | /api/v1/inventory/{id} | 更新库存 |
| POST | /api/v1/inventory/{id}/inbound | 入库操作 |
| POST | /api/v1/inventory/{id}/outbound | 出库操作 |
| GET | /api/v1/inventory/alert | 库存预警列表 |

### 11.8 采购模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/purchase/orders | 获取采购单列表 |
| GET | /api/v1/purchase/orders/{id} | 获取采购单详情 |
| POST | /api/v1/purchase/orders | 创建采购单 |
| PUT | /api/v1/purchase/orders/{id} | 更新采购单 |
| PUT | /api/v1/purchase/orders/{id}/status | 更新采购单状态 |
| GET | /api/v1/purchase/suppliers | 获取供应商列表 |
| POST | /api/v1/purchase/suppliers | 创建供应商 |
| PUT | /api/v1/purchase/suppliers/{id} | 更新供应商 |

### 11.9 财务模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/finance/reports | 获取财务报表列表 |
| GET | /api/v1/finance/reports/{id} | 获取财务报表详情 |
| POST | /api/v1/finance/reports/generate | 生成财务报表 |
| GET | /api/v1/finance/transactions | 获取交易记录 |
| GET | /api/v1/finance/summary | 财务汇总 |
| GET | /api/v1/finance/daily | 日报数据 |

### 11.10 员工模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/employees | 获取员工列表 |
| GET | /api/v1/employees/{id} | 获取员工详情 |
| POST | /api/v1/employees | 创建员工 |
| PUT | /api/v1/employees/{id} | 更新员工 |
| DELETE | /api/v1/employees/{id} | 删除员工 |
| PUT | /api/v1/employees/{id}/status | 更新员工状态 |

### 11.11 设备模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/devices | 获取设备列表 |
| GET | /api/v1/devices/{id} | 获取设备详情 |
| POST | /api/v1/devices | 创建设备 |
| PUT | /api/v1/devices/{id} | 更新设备 |
| DELETE | /api/v1/devices/{id} | 删除设备 |
| PUT | /api/v1/devices/{id}/status | 更新设备状态 |
| GET | /api/v1/devices/{id}/status-log | 设备状态日志 |

### 11.12 系统模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/system/config | 获取系统配置 |
| PUT | /api/v1/system/config | 更新系统配置 |
| GET | /api/v1/system/logs | 获取操作日志 |
| GET | /api/v1/system/dict | 获取数据字典 |
| GET | /api/v1/system/dict/{type} | 按类型获取字典 |

### 11.13 POS模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/pos/terminals | 获取终端列表 |
| POST | /api/v1/pos/transactions | 创建交易 |
| GET | /api/v1/pos/transactions | 获取交易列表 |
| POST | /api/v1/pos/refund | 退款操作 |
| GET | /api/v1/pos/daily-summary | 日结汇总 |

---

## 十二、接口安全规范

### 12.1 认证机制

- 使用JWT令牌进行身份认证
- Token放在请求头`Authorization: Bearer {token}`中
- Token有效期：访问令牌2小时，刷新令牌7天
- Token过期后使用刷新令牌获取新的访问令牌

### 12.2 权限控制

- 基于RBAC（基于角色的访问控制）模型
- 接口级别使用`@PreAuthorize`注解控制访问权限
- 数据级别通过Service层过滤用户可见数据

### 12.3 数据安全

- 敏感字段（密码等）传输时加密
- 禁止在日志中输出敏感信息
- 禁止在错误响应中暴露系统内部信息
- 使用HTTPS传输，生产环境强制HTTPS

### 12.4 请求限流

- 登录接口：同一IP每分钟最多5次
- 普通接口：同一用户每分钟最多60次
- 导出接口：同一用户每分钟最多3次

---

## 十三、DataConverter设计模式【强制】

### 13.1 设计原则

DataConverter是前后端数据转换的标准模式，确保：
- 组件层只处理前端友好的数据格式
- API层只发送后端期望的数据格式
- 转换逻辑集中管理，不散落在各组件中

### 13.2 接口定义

```typescript
interface DataConverter<F, B> {
  toFrontend(backend: B): F;
  toBackend(frontend: F): B;
  toFrontendList(backendList: B[]): F[];
  toBackendList(frontendList: F[]): B[];
}
```

### 13.3 文件位置

Converter文件统一放在 `src/api/{module}/converters.ts`

### 13.4 与API层集成

API方法中必须使用Converter进行数据转换，组件中禁止直接做状态映射或金额转换。详细规范参见 [16-数据转换器规范](16-数据转换器规范.md)。
