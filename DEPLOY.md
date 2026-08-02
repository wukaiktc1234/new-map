# 食品溯源系统 - 部署指南

## 部署方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|-----|------|------|---------|
| **Docker一键部署** | 最简单，自动配置环境 | 需要安装Docker | 推荐，生产环境 |
| **Windows安装脚本** | 无需Docker，本地运行 | 需要手动运行脚本 | Windows开发环境 |
| **手动安装** | 完全控制 | 步骤繁琐 | 自定义需求 |

---

## 方案一：Docker一键部署（推荐）

### 前提条件
- 安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Windows用户需启用WSL2

### 部署步骤

```powershell
# 进入项目目录
cd p:\my-new-project

# 一键启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f paddleocr
```

### 服务地址

| 服务 | 地址 | 说明 |
|-----|------|------|
| 后端API | http://localhost:8081/api | Spring Boot |
| PaddleOCR | http://localhost:8868 | OCR识别服务 |
| MySQL | localhost:3306 | 数据库 |

### 停止服务

```powershell
docker-compose down
```

---

## 方案二：Windows安装脚本

### 步骤

```powershell
# 1. 双击运行安装脚本
install-ocr.bat

# 2. 按提示完成安装

# 3. 启动OCR服务
cd p:\my-new-project\ocr-service
start_ocr.bat
```

### 手动启动

```powershell
# 激活虚拟环境
conda activate paddleocr

# 进入目录
cd p:\my-new-project\ocr-service

# 启动服务
python ocr_server.py
```

---

## 方案三：GPU加速部署

### 前提条件
- NVIDIA显卡（如GTX 1660）
- 安装 [NVIDIA驱动](https://www.nvidia.com/Download/index.aspx)
- 安装 [CUDA Toolkit 11.8+](https://developer.nvidia.com/cuda-downloads)

### Docker GPU部署

```yaml
# docker-compose.yml 中添加GPU配置
services:
  paddleocr:
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    environment:
      - USE_GPU=true
      - GPU_MEM=4000
```

### 本地GPU部署

```powershell
# 安装GPU版本
conda activate paddleocr
pip install paddlepaddle-gpu

# 设置环境变量
set USE_GPU=true
set GPU_MEM=4000

# 启动服务
python ocr_server.py
```

---

## 生产环境部署检查清单

### 服务器要求

| 配置项 | 最低要求 | 推荐配置 |
|-------|---------|---------|
| CPU | 4核 | 8核+ |
| 内存 | 8GB | 16GB+ |
| 硬盘 | 50GB | 100GB+ SSD |
| 显卡 | 无 | GTX 1660+ (可选) |

### 部署前检查

- [ ] Docker已安装并运行
- [ ] 端口8081、8868、3306未被占用
- [ ] 数据库初始化脚本已准备
- [ ] 配置文件已修改（数据库密码等）

### 部署命令

```bash
# 1. 构建镜像
docker-compose build

# 2. 启动服务
docker-compose up -d

# 3. 检查健康状态
curl http://localhost:8868/health
curl http://localhost:8081/api/actuator/health

# 4. 初始化数据库（首次部署）
# 数据库会自动初始化，或手动执行SQL脚本
```

---

## 常见问题

### Q1: Docker启动失败
```bash
# 查看日志
docker-compose logs paddleocr

# 常见原因：
# - 端口被占用：修改docker-compose.yml中的端口映射
# - 内存不足：增加Docker内存限制
```

### Q2: OCR识别慢
- 启用GPU加速
- 增加内存限制
- 使用更快的模型

### Q3: 新电脑部署
```bash
# 方式一：Docker（推荐）
git clone <repository>
cd my-new-project
docker-compose up -d

# 方式二：安装脚本
双击 install-ocr.bat
```

---

## 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      Docker Compose                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐   ┌─────────────────┐   ┌───────────┐ │
│  │  Spring Boot    │   │   PaddleOCR     │   │   MySQL   │ │
│  │  :8081          │──▶│   :8868         │   │   :3306   │ │
│  │                 │   │                 │   │           │ │
│  │  - REST API     │   │  - OCR识别      │   │  - 数据   │ │
│  │  - 业务逻辑     │   │  - 发票解析     │   │  - 存储   │ │
│  └─────────────────┘   └─────────────────┘   └───────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```
