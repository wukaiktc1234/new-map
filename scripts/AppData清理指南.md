# AppData\Local 文件夹清理指南

## 文件夹分析

### 路径
`C:\Users\Liberty\AppData\Local`

### 文件夹数量
约100+个文件夹

### 主要分类

| 分类 | 文件夹数量 | 说明 |
|--------|-----------|------|
| **缓存文件夹** | ~10个 | Temp, cache, npm-cache, pip等 |
| **游戏相关** | ~15个 | Steam, Epic, Battle.net, Blizzard等 |
| **开发工具** | ~20个 | Docker, npm, pip, flutter等 |
| **浏览器相关** | ~5个 | Chrome, Google, Microsoft Edge等 |
| **应用数据** | ~50个 | Adobe, Autodesk, 各种软件数据 |
| **系统相关** | ~10个 | Microsoft, Windows, Diagnostics等 |

## 立即可清理的文件夹（安全）

### 1. Temp - 临时文件
- **路径**: `C:\Users\Liberty\AppData\Local\Temp`
- **作用**: 存储临时文件
- **大小**: 通常100MB-1GB
- **清理建议**: ✅ **立即可删除**
- **影响**: 无影响，系统会自动重新创建

### 2. cache - 应用缓存
- **路径**: `C:\Users\Liberty\AppData\Local\cache`
- **作用**: 各种应用的缓存文件
- **大小**: 通常100MB-500MB
- **清理建议**: ✅ **立即可删除**
- **影响**: 应用会重新生成缓存，首次启动可能稍慢

### 3. npm-cache - NPM包缓存
- **路径**: `C:\Users\Liberty\AppData\Local\npm-cache`
- **作用**: Node.js包管理器缓存
- **大小**: 通常500MB-2GB
- **清理建议**: ✅ **立即可删除**
- **影响**: 需要时自动重新下载包

### 4. pip - Python包缓存
- **路径**: `C:\Users\Liberty\AppData\Local\pip`
- **作用**: Python包管理器缓存
- **大小**: 通常100MB-500MB
- **清理建议**: ✅ **立即可删除**
- **影响**: 需要时自动重新下载包

### 5. Package Cache - Windows包缓存
- **路径**: `C:\Users\Liberty\AppData\Local\Package Cache`
- **作用**: Windows安装包缓存
- **大小**: 通常500MB-1GB
- **清理建议**: ✅ **立即可删除**
- **影响**: 安装软件时会重新下载

### 6. INetCache - IE浏览器缓存
- **路径**: `C:\Users\Liberty\AppData\Local\Microsoft\Windows\INetCache`
- **作用**: Internet Explorer缓存
- **大小**: 通常50MB-200MB
- **清理建议**: ✅ **立即可删除**
- **影响**: 浏览器会重新生成缓存

### 7. Chrome Cache - Chrome浏览器缓存
- **路径**: `C:\Users\Liberty\AppData\Local\Google\Chrome\User Data\Default\Cache`
- **作用**: Chrome浏览器缓存
- **大小**: 通常200MB-1GB
- **清理建议**: ✅ **立即可删除**
- **影响**: 浏览器会重新生成缓存

## 谨慎删除的文件夹

### 游戏相关文件夹

| 文件夹 | 作用 | 清理建议 |
|--------|------|-----------|
| Steam | Steam游戏数据 | ⚠️ 只删除不玩的游戏 |
| Epic Games | Epic游戏数据 | ⚠️ 只删除不玩的游戏 |
| Battle.net | 暴雪游戏数据 | ⚠️ 只删除不玩的游戏 |
| Blizzard Entertainment | 暴雪游戏数据 | ⚠️ 只删除不玩的游戏 |
| Origin | EA游戏平台 | ⚠️ 只删除不玩的游戏 |
| GOG.com | GOG游戏数据 | ⚠️ 只删除不玩的游戏 |
| Rockstar Games | Rockstar游戏数据 | ⚠️ 只删除不玩的游戏 |

### 开发工具文件夹

| 文件夹 | 作用 | 清理建议 |
|--------|------|-----------|
| Docker | Docker数据 | ⚠️ 确认不使用Docker |
| flutter_webview_windows | Flutter开发 | ⚠️ 确认不使用Flutter |
| ms-playwright | 测试工具 | ⚠️ 确认不使用Playwright |
| UniCompactView | 工具软件 | ⚠️ 确认不使用 |

### 应用数据文件夹

| 文件夹 | 作用 | 清理建议 |
|--------|------|-----------|
| Adobe | Adobe软件数据 | ⚠️ 删除前关闭Adobe软件 |
| Autodesk | Autodesk软件数据 | ⚠️ 删除前关闭Autodesk软件 |
| NVIDIA Corporation | NVIDIA驱动数据 | ⚠️ 删除前确认不需要 |

## 清理步骤

### 步骤1: 使用自动化脚本

```batch
cd p:\my-new-project\scripts
clean-appdata-local.bat
```

脚本会自动：
1. 分析常见缓存文件夹大小
2. 显示清理建议
3. 询问是否清理每个文件夹
4. 执行清理操作

### 步骤2: 手动清理缓存

#### 清理 Temp 文件夹
```batch
rd /s /q "C:\Users\Liberty\AppData\Local\Temp"
```

#### 清理 cache 文件夹
```batch
rd /s /q "C:\Users\Liberty\AppData\Local\cache"
```

#### 清理 npm-cache 文件夹
```batch
rd /s /q "C:\Users\Liberty\AppData\Local\npm-cache"
```

#### 清理 pip 文件夹
```batch
rd /s /q "C:\Users\Liberty\AppData\Local\pip"
```

#### 清理 Package Cache 文件夹
```batch
rd /s /q "C:\Users\Liberty\AppData\Local\Package Cache"
```

#### 清理 INetCache 文件夹
```batch
rd /s /q "C:\Users\Liberty\AppData\Local\Microsoft\Windows\INetCache"
```

#### 清理 Chrome Cache 文件夹
```batch
rd /s /q "C:\Users\Liberty\AppData\Local\Google\Chrome\User Data\Default\Cache"
```

### 步骤3: 清理游戏文件夹（可选）

**⚠️ 警告**: 只删除不玩的游戏文件夹

```batch
# 示例：删除不玩的Steam游戏
rd /s /q "C:\Users\Liberty\AppData\Local\Steam\steamapps\common\不玩的游戏"
```

### 步骤4: 清理开发工具文件夹（可选）

**⚠️ 警告**: 只删除不使用的工具文件夹

```batch
# 示例：删除不使用的Docker数据
rd /s /q "C:\Users\Liberty\AppData\Local\Docker"
```

## 预期释放空间

| 文件夹 | 预期大小 |
|--------|----------|
| Temp | 100MB-1GB |
| cache | 100MB-500MB |
| npm-cache | 500MB-2GB |
| pip | 100MB-500MB |
| Package Cache | 500MB-1GB |
| INetCache | 50MB-200MB |
| Chrome Cache | 200MB-1GB |
| **总计（保守估计）** | **~1.5GB-6GB** |
| **总计（乐观估计）** | **~3GB-10GB** |

## 验证清理

### 检查C盘空间

```powershell
# 查看C盘空间
Get-PSDrive C | Select-Object Used, Free

# 或者使用系统属性
# 右键点击C盘 -> 属性
```

### 验证应用正常运行

清理后验证常用应用：
- [ ] Chrome浏览器正常
- [ ] Steam正常
- [ ] 开发工具正常
- [ ] 其他常用应用正常

## 常见问题

### Q1: 清理后应用无法启动？

**A**: 
1. 检查应用是否需要重新配置
2. 重启应用
3. 如果仍然无法启动，从备份恢复

### Q2: 清理后游戏数据丢失？

**A**: 
- 游戏文件夹不应该被清理（除非手动删除）
- 如果误删，从Steam/Epic等平台重新下载

### Q3: 清理后npm/pip命令失败？

**A**: 
- 缓存会自动重新生成
- 首次运行可能稍慢（需要重新下载包）

### Q4: 如何查看文件夹大小？

**A**: 
```powershell
# 查看单个文件夹大小
Get-ChildItem "C:\Users\Liberty\AppData\Local\Temp" -Recurse | Measure-Object -Property Length -Sum

# 查看所有文件夹大小
Get-ChildItem "C:\Users\Liberty\AppData\Local" | ForEach-Object {
    $size = (Get-ChildItem $_.FullName -Recurse -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
    Write-Host "$($_.Name): $([math]::Round($size/1MB,2)) MB"
}
```

## 高级清理

### 清理所有临时文件

```powershell
# 清理所有Temp文件夹
Get-ChildItem "C:\Users\Liberty\AppData\Local" | Where-Object { $_.Name -like "*Temp*" } | ForEach-Object {
    Remove-Item $_.FullName -Recurse -Force
}
```

### 清理所有缓存文件夹

```powershell
# 清理所有cache文件夹
Get-ChildItem "C:\Users\Liberty\AppData\Local" | Where-Object { $_.Name -like "*cache*" } | ForEach-Object {
    Remove-Item $_.FullName -Recurse -Force
}
```

### 清理大于100MB的文件夹

```powershell
# 查找并清理大于100MB的文件夹
Get-ChildItem "C:\Users\Liberty\AppData\Local" | ForEach-Object {
    $size = (Get-ChildItem $_.FullName -Recurse -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
    if ($size -gt 100MB) {
        Write-Host "$($_.Name): $([math]::Round($size/1MB,2)) MB"
    }
}
```

## 清理检查清单

### 清理前
- [ ] 确认不玩的游戏列表
- [ ] 确认不使用的开发工具
- [ ] 备份重要数据（如果有）

### 清理后
- [ ] Temp 文件夹已删除
- [ ] cache 文件夹已删除
- [ ] npm-cache 文件夹已删除
- [ ] pip 文件夹已删除
- [ ] Package Cache 文件夹已删除
- [ ] INetCache 文件夹已删除
- [ ] Chrome Cache 文件夹已删除
- [ ] C盘空间已释放
- [ ] 常用应用正常运行

## 总结

### 立即可清理（推荐）
1. ✅ Temp 文件夹（100MB-1GB）
2. ✅ cache 文件夹（100MB-500MB）
3. ✅ npm-cache 文件夹（500MB-2GB）
4. ✅ pip 文件夹（100MB-500MB）
5. ✅ Package Cache 文件夹（500MB-1GB）
6. ✅ INetCache 文件夹（50MB-200MB）
7. ✅ Chrome Cache 文件夹（200MB-1GB）

### 谨慎清理（可选）
8. ⚠️ 不玩的游戏文件夹
9. ⚠️ 不使用的开发工具文件夹
10. ⚠️ 不使用的应用数据文件夹

### 预期释放空间
- **保守估计**: ~1.5GB-6GB
- **乐观估计**: ~3GB-10GB

## 下一步行动

1. **立即执行**: 运行 `clean-appdata-local.bat`
2. **验证**: 确认C盘空间释放
3. **测试**: 验证常用应用正常运行
4. **可选**: 清理不玩的游戏和不使用的工具
