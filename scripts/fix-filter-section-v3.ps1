# 改进版批量修复脚本 v3
$ErrorActionPreference = "Stop"
$viewsDir = "p:\my-new-project\frontend\src\views"

$fixedCount = 0
$skippedCount = 0

Write-Host "开始批量修复 fts-filter-section 位置 (v3)..." -ForegroundColor Cyan

Get-ChildItem -Path $viewsDir -Recurse -Filter "*.vue" | ForEach-Object {
    $filePath = $_.FullName
    
    try {
        $content = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)
        
        if ($content -notmatch 'fts-filter-section') { return }
        
        $filterStart = $content.IndexOf('<div class="fts-filter-section">')
        if ($filterStart -eq -1) { return }
        
        # 检查是否在外部
        $beforeContent = $content.Substring(0, $filterStart)
        $openCards = ([regex]::Matches($beforeContent, '<ContentCard[\s>]')).Count
        $closeCards = ([regex]::Matches($beforeContent, '</ContentCard>')).Count
        
        if ($openCards -gt $closeCards) { 
            $skippedCount++
            return 
        }
        
        # 找 filter-section 结束位置
        $depth = 0
        $i = $filterStart
        $filterEnd = -1
        
        while ($i -lt $content.Length) {
            if ($i + 26 -lt $content.Length -and $content.Substring($i, 27) -eq '<div class="fts-filter-section"') {
                $depth++
            } elseif ($i + 5 -lt $content.Length -and $content.Substring($i, 6) -eq '</div>') {
                $depth--
                if ($depth -eq 0) {
                    $filterEnd = $content.IndexOf('>', $i) + 1
                    break
                }
            }
            $i++
        }
        
        if ($filterEnd -eq -1) { return }
        
        # 查找后面的 ContentCard
        $afterFilterPos = $content.IndexOf('<ContentCard', $filterEnd)
        if ($afterFilterPos -eq -1) { return }
        
        # 找插入点
        $afterCard = $content.Substring($afterFilterPos, [Math]::Min(600, $content.Length - $afterFilterPos))
        $insertOffset = 0
        
        $templateMatches = [regex]::Matches($afterCard, '</template>')
        if ($templateMatches.Count -gt 0) {
            $lastIdx = $templateMatches[$templateMatches.Count - 1].Index + $templateMatches[$templateMatches.Count - 1].Length
            $insertOffset = $lastIdx
        } else {
            $tagMatch = [regex]::Match($afterCard, '<ContentCard[^>]*>')
            if ($tagMatch.Success) {
                $insertOffset = $tagMatch.Index + $tagMatch.Length
            }
        }
        
        if ($insertOffset -eq 0) { return }
        
        $insertPos = $afterFilterPos + $insertOffset
        
        # 构建新内容
        $part1 = $content.Substring(0, $filterStart).TrimEnd()
        $part2 = $content.Substring($filterEnd, insertPos - $filterEnd)
        $part3 = $content.Substring($insertPos)
        
        $filterBlock = $content.Substring($filterStart, $filterEnd - $filterStart).Trim()
        
        # 缩进处理
        $lines = $filterBlock -split "`n"
        $indentedLines = @("      ") + ($lines | ForEach-Object { "        $_" })
        $indentedFilter = $indentedLines -join "`r`n"
        
        $newContent = "$part1`r`n`r`n$part2`r`n$indentedFilter`r`n$part3"
        
        [System.IO.File]::WriteAllText($filePath, $newContent, [System.Text.Encoding]::UTF8)
        $fixedCount++
        $relPath = $filePath.Replace($viewsDir, "").Replace("\", "/")
        Write-Host "[FIXED] $relPath" -ForegroundColor Green
        
    } catch {
        Write-Host "[ERROR] $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n========== 完成: 已修复=$fixedCount, 跳过=$skippedCount ==========" -ForegroundColor Cyan
