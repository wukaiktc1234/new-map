# Skill: 数据库变更

> 触发条件：需要修改数据库表结构、新增表、修改字段等

## 变更流程（必须按顺序执行）

### Phase 1: 设计
```
1. 明确变更内容:
   - [ ] 新增表? → 设计完整 DDL
   - [ ] 新增字段? → 确定类型、默认值、是否允许 NULL
   - [ ] 修改字段? → 评估数据迁移风险
   - [ ] 删除字段? → 改为逻辑删除 (deleted=1)

2. 命名规范检查:
   - 表名: 小写+下划线 (food_trace)
   - 主键: {table}_id, SERIAL 或 IDENTITY 自增
   - 外键: {referenced_table}_id
   - 时间字段: {action}_time (TIMESTAMP)
   - 逻辑删除: deleted TINYINT DEFAULT 0 (必需!)
```

### Phase 2: H2 开�发环境
```
文件: config/DatabaseInitConfig.java

操作:
1. 创建/修改 createXxxTable() 方法
   - 使用 CREATE TABLE IF NOT EXISTS
   - 包含所有必要字段
   - deleted TINYINT DEFAULT 0 (每张表必备!)

2. 在 initDevDatabase() 中调用新方法
   - 添加在合适的表组位置

3. 如有默认数据:
   - 创建 insertDefaultXxxData() 方法
   - 在 insertDefaultData() 中调用

⚠️ H2 兼容性注意事项:
- 不要用 ENGINE=InnoDB
- 不要用 UNIQUE KEY (用 UNIQUE 约束代替)
- 不要用 CHARSET=utf8mb4
- 不要用 AUTO_INCREMENT (用 GENERATED ALWAYS AS IDENTITY)
- 不要用 ON UPDATE CURRENT_TIMESTAMP (H2不完全支持)
- 字符串连接用 || (不是 CONCAT)
```

### Phase 3: Entity 层同步
```
文件: entity/XxxEntity.java

检查:
- [ ] @TableName 与实际表名一致
- [ ] 所有 DB 字段都有对应 Java 字段
- [ ] @TableField("column_name") 映射正确
- [ ] @TableLogic 标注 deleted 字段
- [ ] 显式 getter/setter (Delombok 项目!)
- [ ] 时间字段类型: LocalDateTime (不是 Date)
```

### Phase 4: Mapper 层同步
```
如需自定义 SQL:
1. 检查 mapper/XxxMapper.xml
2. 确保列名与 Entity @TableField 匹配
3. 避免 MySQL 专有函数
```

### Phase 5: 编译验证
```bash
cd backend && mvn compile -q
# 重启后端观察启动日志有无 SQL 错误
```

## PostgreSQL 迁移准备（未来需要时）
```
当切换到 PostgreSQL 时:
1. 将所有 CREATE TABLE 语句写入 Flyway 迁移脚本
2. 文件命名: V{N}__Description.sql
3. 注意 PG 与 H2 的差异:
   - SERIAL vs IDENTITY
   - TEXT vs CLOB
   - now() vs CURRENT_TIMESTAMP
   - boolean 类型大小写
```

## 完成标志
- [ ] mvn compile 成功
- [ ] 后端启动无 Table not found 错误
- [ ] 新表的 CRUD 操作正常
- [ ] PROJECT_PROGRESS.md 已记录变更
