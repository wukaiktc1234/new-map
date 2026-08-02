# 食品溯源系统部署指南

## 1. 系统概述

食品溯源系统是一个基于Spring Boot 3.2.0和Vue 3的企业级应用，用于管理食品的采购、仓储和财务流程。本指南提供了生产环境的部署说明，包括系统要求、环境准备、部署步骤和运维建议。

## 2. 系统要求

### 2.1 硬件要求

| 环境 | CPU | 内存 | 磁盘空间 | 网络 |
|------|-----|------|----------|------|
| 生产环境 | 4核及以上 | 8GB及以上 | 100GB及以上 | 千兆网卡 |
| 测试环境 | 2核 | 4GB | 50GB | 百兆网卡 |

### 2.2 软件要求

| 软件 | 版本 | 用途 |
|------|------|------|
| Java | 17或以上 | 运行Spring Boot应用 |
| Maven | 3.6.0或以上 | 构建项目 |
| MySQL | 8.0或以上 | 数据库 |
| Windows Server | 2016或以上 | 服务器操作系统 |

## 3. 环境准备

### 3.1 安装Java

1. 下载Java 17或以上版本的JDK
2. 安装JDK并配置环境变量
3. 验证安装：
   ```bash
   java -version
   ```

### 3.2 安装Maven

1. 下载Maven 3.6.0或以上版本
2. 解压并配置环境变量
3. 验证安装：
   ```bash
   mvn -version
   ```

### 3.3 安装MySQL

1. 下载MySQL 8.0或以上版本
2. 安装并配置MySQL服务器
3. 创建数据库：
   ```sql
   CREATE DATABASE food_traceability CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
4. 创建数据库用户并授权：
   ```sql
   CREATE USER 'food_user'@'%' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON food_traceability.* TO 'food_user'@'%';
   FLUSH PRIVILEGES;
   ```

## 4. 项目构建

### 4.1 克隆代码

```bash
git clone <repository-url>
cd my-new-project/backend
```

### 4.2 配置环境变量

1. 复制环境变量示例文件：
   ```bash
   copy .env.example .env
   ```
2. 编辑.env文件，配置必要的环境变量：
   ```
   # 数据库配置
   DB_URL=jdbc:mysql://localhost:3306/food_traceability?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8
   DB_USER=food_user
   DB_PASSWORD=your_password

   # JWT配置
   JWT_SECRET=your_jwt_secret_key
   JWT_EXPIRATION=86400000

   # 其他配置
   MAIL_HOST=smtp.example.com
   MAIL_PORT=587
   MAIL_USERNAME=your_email@example.com
   MAIL_PASSWORD=your_email_password
   ```

### 4.3 构建项目

使用提供的构建脚本：

```bash
cd scripts
build-prod.bat
```

或者手动构建：

```bash
mvn clean package -DskipTests
```

构建成功后，JAR包将生成在`target`目录下。

## 5. 部署步骤

### 5.1 启动应用

使用提供的启动脚本：

```bash
cd scripts
start-prod.bat
```

或者手动启动：

```bash
java -jar target/food-traceability-1.0.0.jar --spring.profiles.active=prod
```

### 5.2 验证部署

1. 检查应用日志：
   ```bash
tail -f logs/backend-prod.log
   ```
2. 访问应用：
   - API文档：http://your-server-ip:8083/swagger-ui.html
   - 前端应用：http://your-server-ip:8083

## 6. 配置管理

### 6.1 配置文件结构

- `application.yml`：基础配置
- `application-prod.yml`：生产环境配置
- `.env`：环境变量配置

### 6.2 关键配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | 8083 |
| `spring.datasource.url` | 数据库连接URL | - |
| `spring.datasource.username` | 数据库用户名 | - |
| `spring.datasource.password` | 数据库密码 | - |
| `jwt.secret` | JWT密钥 | - |
| `jwt.expiration` | JWT过期时间 | 86400000 |

### 6.3 环境变量

所有关键配置项都可以通过环境变量覆盖，例如：

```bash
set DB_PASSWORD=your_new_password
java -jar target/food-traceability-1.0.0.jar --spring.profiles.active=prod
```

## 7. 运维管理

### 7.1 日志管理

- 日志文件位置：`logs/backend-prod.log`
- 日志级别配置：在`application-prod.yml`中修改

### 7.2 监控管理

- 健康检查：http://your-server-ip:8083/actuator/health
- 指标监控：http://your-server-ip:8083/actuator/metrics
- Prometheus端点：http://your-server-ip:8083/actuator/prometheus

### 7.3 备份策略

1. 数据库备份：
   ```bash
   mysqldump -u food_user -p food_traceability > food_traceability_backup_$(date +%Y%m%d).sql
   ```

2. 配置文件备份：
   ```bash
   copy .env .env_$(date +%Y%m%d)
   copy application-prod.yml application-prod_$(date +%Y%m%d).yml
   ```

### 7.4 常见运维任务

| 任务 | 命令 |
|------|------|
| 启动应用 | `scripts/start-prod.bat` |
| 停止应用 | `scripts/stop-prod.bat` |
| 重启应用 | `scripts/stop-prod.bat && scripts/start-prod.bat` |
| 查看应用状态 | `netstat -ano | findstr :8083` |
| 查看日志 | `tail -f logs/backend-prod.log` |

## 8. 安全最佳实践

### 8.1 网络安全

1. 使用防火墙限制访问端口
2. 配置SSL证书，使用HTTPS访问
3. 定期更新系统和依赖包

### 8.2 数据库安全

1. 使用强密码
2. 限制数据库用户权限
3. 定期备份数据库
4. 启用数据库审计日志

### 8.3 应用安全

1. 定期更新JWT密钥
2. 配置适当的CORS策略
3. 启用安全头（XSS、CSP、HSTS）
4. 禁用不必要的生产环境功能（如Swagger）

## 9. 故障排除

### 9.1 常见问题

| 问题 | 可能原因 | 解决方案 |
|------|----------|----------|
| 应用启动失败 | 数据库连接错误 | 检查数据库配置和网络连接 |
| 端口被占用 | 其他应用占用了8083端口 | 停止占用端口的应用或修改配置文件中的端口 |
| 权限错误 | 数据库用户权限不足 | 检查数据库用户权限 |
| 日志中出现OutOfMemoryError | 内存不足 | 增加JVM内存配置 |

### 9.2 排查步骤

1. 检查应用日志：
   ```bash
tail -f logs/backend-prod.log
   ```

2. 检查系统资源：
   ```bash
   tasklist | findstr java
   taskmgr
   ```

3. 检查数据库连接：
   ```bash
   mysql -u food_user -p -h localhost food_traceability
   ```

4. 检查网络连接：
   ```bash
   ping localhost
   telnet localhost 3306
   ```

## 10. 性能优化

### 10.1 数据库优化

1. 确保所有查询都有适当的索引
2. 优化SQL查询，避免全表扫描
3. 配置适当的连接池大小

### 10.2 应用优化

1. 启用缓存机制
2. 优化JVM参数：
   ```bash
   java -Xms4g -Xmx8g -jar target/food-traceability-1.0.0.jar
   ```
3. 定期清理日志和临时文件

## 11. 升级流程

### 11.1 备份数据

```bash
# 备份数据库
mysqldump -u food_user -p food_traceability > food_traceability_backup_$(date +%Y%m%d).sql

# 备份配置文件
copy .env .env_backup
copy application-prod.yml application-prod_backup.yml
```

### 11.2 停止旧版本应用

```bash
scripts/stop-prod.bat
```

### 11.3 部署新版本

```bash
# 拉取最新代码
git pull

# 构建项目
scripts/build-prod.bat

# 启动新版本
scripts/start-prod.bat
```

### 11.4 验证升级

1. 检查应用日志
2. 访问应用，验证功能正常
3. 执行必要的测试用例

## 12. 联系方式

如果遇到部署或运维问题，请联系技术支持团队：

- 邮箱：support@example.com
- 电话：+86-123-4567-8910

## 13. 附录

### 13.1 配置文件说明

- `application.yml`：基础配置，包含数据库连接池、MyBatis Plus等配置
- `application-prod.yml`：生产环境专用配置，包含日志级别、安全配置等
- `.env`：环境变量配置，包含敏感信息如数据库密码、JWT密钥等

### 13.2 脚本说明

- `scripts/build-prod.bat`：生产环境构建脚本
- `scripts/start-prod.bat`：生产环境启动脚本
- `scripts/stop-prod.bat`：生产环境停止脚本

### 13.3 端口说明

| 端口 | 用途 |
|------|------|
| 8083 | 应用访问端口 |
| 3306 | MySQL数据库端口 |

---

**文档版本**：1.0
**发布日期**：2026-01-17
**编写团队**：技术支持团队
