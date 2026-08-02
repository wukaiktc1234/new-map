# 批量修复脚本：将 fts-filter-section 从 ContentCard 外部移入内部
# 使用方法: powershell -ExecutionPolicy Bypass -File scripts/fix-filter-section.ps1

$ErrorActionPreference = "Stop"
$viewsDir = "p:\my-new-project\frontend\src\views"

$stats = @{
    Total = 0
    Fixed = 0
    Skipped = 0
    Errors = @()
}

Write-Host "开始批量修复 fts-filter-section 位置..." -ForegroundColor Cyan

function Test-NeedsFix {
    param([string]$Content)
    
    # 查找 fts-filter-section
    $filterStart = $Content.IndexOf('<div class="fts-filter-section">')
    if ($filterStart -eq -1) { return $false }
    
    # 检查 filter-section 前面是否有未关闭的 ContentCard
    $beforeFilter = $Content.Substring(0, $filterStart)
    $openCards = ([regex]::Matches($beforeFilter, "<ContentCard")).Count
    $closeCards = ([regex]::Matches($beforeFilter, "</ContentCard>")).Count
    
    # 如果 openCards <= closeCards，说明 filter-section 在 ContentCard 外面
    return $openCards -le $closeCards
}

function Get-FilterSectionBlock {
    param([string]$Content, [int]$StartPos)
    
    $depth = 0
    $i = $StartPos
    $inTag = $false
    
    while ($i -lt $Content.Length) {
        if ($Content[$i] -eq '<' -and $i+1 -lt $Content.Length -and $Content[$i+1] -ne '/') {
            $inTag = $true
            if ($Content.Substring($i, [Math]::Min(27, $Content.Length - $i)) -eq '<div class="fts-filter-section"' -or 
                ($depth -gt 0 -and $Content.Substring($i, [Math]::Min(4, $Content.Length - $i)) -eq '<div')) {
                $depth++
            }
        } elseif ($Content[$i] -eq '>' -and $inTag) {
            $inTag = $false
        } elseif ($i+5 -lt $Content.Length -and $Content.Substring($i, 6) -eq '</div>') {
            $depth--
            if ($depth -eq 0) {
                $endDiv = $Content.IndexOf('>', $i)
                return @{
                    Start = $StartPos
                    End = $endDiv + 1
                    Content = $Content.Substring($StartPos, $endDiv - $StartPos + 1)
                }
            }
        }
        $i++
    }
    return $null
}

function Fix-File {
    param([string]$FilePath)
    
    try {
        $stats.Total++
        $content = [System.IO.File]::ReadAllText($FilePath)
        
        if (-not (Test-NeedsFix $content)) {
            $stats.Skipped++
            return $null
        }
        
        # 提取 filter-section 块
        $filterStart = $content.IndexOf('<div class="fts-filter-section">')
        $filterBlock = Get-FilterSectionBlock -Content $content -StartPos $filterStart
        
        if ($null -eq $filterBlock) {
            $stats.Skipped++
            return $null
        }
        
        # 查找 filter-section 后的第一个 ContentCard
        $afterFilter = $content.Substring($filterBlock.End)
        $cardMatch = [regex]::Match($afterFilter, '<ContentCard[\s>]')
        
        if (-not $cardMatch.Success) {
            $stats.Skipped++
            return $null
        }
        
        $contentCardPos = $filterBlock.End + $cardMatch.Index
        
        # 查找插入位置（在 #actions 或 #header 之后）
        $afterCard = $content.Substring($contentCardPos, [Math]::Min(500, $content.Length - $contentCardPos))
        $insertPos = $contentCardPos
        
        # 尝试找到 </template> 后的位置
        $templateEnds = [regex]::Matches($afterCard, '</template>\s*\r?\n\s*')
        if ($templateEnds.Count -gt 0) {
            $lastTemplateEnd = $templateEnds[$templateEnds.Count - 1]
            $insertPos = $contentCardPos + $lastTemplateEnd.Index + $lastTemplateEnd.Length
        } else {
            # 尝试找到 ContentCard 标签结束后的位置
            $tagEndMatch = [regex]::Match($afterCard, '<ContentCard[^>]*>')
            if ($tagEndMatch.Success) {
                $insertPos = $contentCardPos + $tagEndMatch.Index + $tagEndMatch.Length
            }
        }
        
        # 构建新内容
        $beforeFilter = $content.Substring(0, $filterBlock.Start).TrimEnd()
        $between = $content.Substring($filterBlock.End, $insertPos - $filterBlock.End)
        $afterInsert = $content.Substring($insertPos)
        
        # 格式化 filter-section 内容（添加缩进）
        $filterLines = $filterBlock.Content.Trim() -split "`n"
        $indentedFilter = ($filterLines | ForEach-Object { "      $_" }) -join "`n"
        
        $newContent = "$beforeFilter`n`n$between`n$indentedFilter`n$afterInsert"
        
        [System.IO.File]::WriteAllText($FilePath, $newContent, [System.Text.Encoding]::UTF8)
        $stats.Fixed++
        
        return $FilePath.Replace($viewsDir, "").Replace("\", "/")
    } catch {
        $stats.Errors += @{ File = $FilePath; Error = $_.Exception.Message }
        return $null
    }
}

# 遍历所有 .vue 文件
Get-ChildItem -Path $viewsDir -Recurse -Filter "*.vue" | ForEach-Object {
    $result = Fix-File -FilePath $_.FullName
    if ($result) {
        Write-Host "[已修复] $result" -ForegroundColor Green
    }
}

Write-Host "`n========== 修复完成 ==========" -ForegroundColor Cyan
Write-Host "总文件数: $($stats.Total)"
Write-Host "已修复: $($stats.Fixed)" -ForegroundColor Green
Write-Host "跳过: $($stats.Skipped)"

if ($stats.Errors.Count -gt 0) {
    Write-Host "`n错误:" -ForegroundColor Red
    $stats.Errors | ForEach-Object {
        Write-Host "  - $($_.File): $($_.Error)" -ForegroundColor Red
    }
}
