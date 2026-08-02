# 食品溯源系统 - 安装程序说明

## 版本信息

- 版本: 1.0.0
- 更新日期: 2024-03-24
- 支持系统: Windows 10/11, Windows Server 2016+, Linux (Ubuntu 18.04+)
- 数据库:PostgerSQL 18

## 安装要求

### 最低配置

| 组件  | 要求         |
| --- | ---------- |
| CPU | 双核 2.0GHz+ |
| 内存  | 4GB        |
| 磁盘  | 20GB       |
| 网络  | 可用         |

### 推荐配置

| 组件  | 要求                    |
| --- | --------------------- |
| CPU | 4核 3.0GHz+            |
| 内存  | 8GB+                  |
| 磁盘  | 50GB SSD              |
| 显卡  | GTX 1660 (可选，用于GPU加速) |

## 安装步骤

### 方式一：一键安装（推荐）

```powershell
# 1. 以管理员身份运行
.\installer\install.bat

# 2. 按提示完成安装
# 3. 安装完成后，双击 start.bat 启动服务
```

### 方式二：手动安装

```powershell
# 1. 安装Java 17
#    下载: https://adoptium.net/
#    安装后设置环境变量 JAVA_HOME

# 2. 安装Python 3.10
#    下载: https://www.python.org/downloads/
#    安装时勾选 "Add Python to PATH"
# 3. 安装OCR服务
#    cd ocr-service
#    python -m venv venv
#    venv\Scripts\activate
#    pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
#    python ocr_server.py
# 4. 安装MySQL（可选）
#    下载: https://dev.mysql.com/downloads/installer/
#    安装时设置root密码
# 5. 配置数据库连接
#    修改 backend/src/main/resources/application-prod.yml
# 6. 启动服务
#    双击 start.bat
```

## 安装后目录结构

```
my-new-project/
├── installer/
│   ├── install.bat          # 一键安装程序
│   ├── uninstall.bat        # 卸载程序
│   └── README.md            # 本说明文件
├── backend/
│   ├── src/main/java/...    # Java后端代码
│   ├── target/                 # 编译输出
│   │   └── food-traceability-1.0.0.jar
│   └── start_service.bat     # 后端启动脚本
├── frontend/
│   └── ...                    # Vue前端代码
├── ocr-service/
│   ├── ocr_server.py        # OCR服务主程序
│   ├── start_service.bat     # OCR启动脚本
│   ├── requirements.txt      # Python依赖
│   └── venv/                   # 虚拟环境目录（安装后生成）
├── start.bat                  # 一键启动所有服务
├── stop.bat                   # 一键停止所有服务
├── DEPLOY.md                 # 部署指南
├── DOCKER-MIRROR.md          # Docker镜像问题解决
└── install.log                 # 安装日志（安装时生成）
```

## 配置文件

### 后端配置 (application-prod.yml)

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/food_traceability
    username: root
    password: 123456  # 请修改为实际密码

ocr:
  paddle:
    enabled: true
    url: http://localhost:8868
```

### OCR配置 (ocr-service/config.ini)

```ini
[server]
host = 0.0.0.0
port = 8868

[ocr]
use_gpu = false
gpu_mem = 2000
```

## 巻加到开机自启

### Windows

1. 按 `Win + R` 打开运行对话框
2. 输入 `shell:startup`
3. 输入本安装目录下的 `start.bat`
4. 点击"确定"

### Linux (systemd)

```bash
# 创建服务文件
sudo cp installer/food-traceability.service /etc/systemd/system/
sudo cp installer/paddleocr.service /etc/systemd/system/

# 启用服务
sudo systemctl enable food-traceability
sudo systemctl enable paddleocr

# 启动服务
sudo systemctl start food-traceability
sudo systemctl start paddleocr
```

## 巻加桌面快捷方式

安装完成后，桌面上会自动创建以下快捷方式：

- **食品溯源系统** - 启动服务
- **食品溯源系统** - 埥看日志
- **OCR服务** - 启动OCR
- **OCR服务** - 查看日志

***

## 声明

本安装程序会自动检测并安装所需的依赖环境，无需用户手动干预。

安装完成后，可以通过桌面快捷方式快速启动服务
