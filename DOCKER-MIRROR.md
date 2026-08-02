# Docker镜像加速配置指南

## 问题说明
由于政策原因，大部分国内Docker镜像源已失效。以下是当前可用的解决方案。

---

## 方案一：使用Docker官方镜像（需要科学上网）

### 1. 配置代理
在Docker Desktop设置中：
1. 打开 Settings -> Resources -> Proxies
2. 启用 Manual proxy configuration
3. 填写代理地址

### 2. 或使用系统代理
```powershell
# 设置环境变量
$env:HTTP_PROXY = "http://127.0.0.1:7890"
$env:HTTPS_PROXY = "http://127.0.0.1:7890"
```

---

## 方案二：使用阿里云个人镜像加速

### 1. 获取专属加速地址
1. 登录 [阿里云容器镜像服务](https://cr.console.aliyun.com/)
2. 左侧菜单 -> 镜像工具 -> 镜像加速器
3. 复制您的专属加速地址（格式：https://xxxxxx.mirror.aliyuncs.com）

### 2. 配置Docker Desktop
```json
// C:\Users\你的用户名\.docker\daemon.json
{
  "registry-mirrors": [
    "https://你的专属ID.mirror.aliyuncs.com"
  ]
}
```

---

## 方案三：不使用Docker，直接本地部署

### Windows本地部署（推荐）

```powershell
# 1. 运行安装脚本
.\install-ocr.bat

# 2. 启动OCR服务
cd .\ocr-service
.\start_ocr.bat
```

### 优点
- 无需Docker
- 无需下载大镜像
- 安装更快

---

## 方案四：使用预构建的离线包

### 创建离线安装包

在有网络的电脑上：
```powershell
# 下载Python安装包
# 下载PaddleOCR模型文件
# 打包成zip
```

### 在目标电脑上
```powershell
# 解压后直接运行
.\offline-install\install.bat
```

---

## 推荐方案

| 场景 | 推荐方案 |
|-----|---------|
| 有代理/VPN | Docker官方镜像 |
| 阿里云账号 | 阿里云个人镜像加速 |
| 无代理、无云账号 | **本地部署（推荐）** |
| 内网环境 | 离线安装包 |

---

## 本地部署详细步骤

### 步骤1：安装Python
```powershell
# 下载Python 3.10
# https://www.python.org/downloads/release/python-31011/

# 或使用winget
winget install Python.Python.3.10
```

### 步骤2：创建虚拟环境
```powershell
python -m venv paddleocr-env
.\paddleocr-env\Scripts\activate
```

### 步骤3：安装依赖
```powershell
# CPU版本（无需显卡）
pip install paddlepaddle paddleocr flask flask-cors -i https://pypi.tuna.tsinghua.edu.cn/simple

# GPU版本（有NVIDIA显卡）
pip install paddlepaddle-gpu paddleocr flask flask-cors -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 步骤4：启动服务
```powershell
cd p:\my-new-project\ocr-service
python ocr_server.py
```

---

## 常见问题

### Q: pip安装也慢怎么办？
```powershell
# 使用清华源
pip install xxx -i https://pypi.tuna.tsinghua.edu.cn/simple

# 或配置永久使用
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
```

### Q: 没有显卡能用吗？
可以！CPU模式完全够用，只是速度稍慢（识别一张发票约2-5秒）。

### Q: 需要联网吗？
- 安装时：需要（下载依赖和模型）
- 运行时：不需要（本地运行）
