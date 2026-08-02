# 改进版批量修复脚本 v2
# 更精确地识别和修复 fts-filter-section 位置

$ErrorActionPreference = "Stop"
$viewsDir = "p:\my-new-project\frontend\src\views"

$fixedCount = 0
$skippedCount = 0
$errorFiles = @()

Write-Host "开始批量修复 fts-filter-section 位置 (v2)..." -ForegroundColor Cyan

function Fix-File-If-Needed {
    param([string]$FilePath)
    
    try {
        $content = [System.IO.File]::ReadAllText($FilePath, [System.Text.Encoding]::UTF8)
        
        # 检查是否包含 fts-filter-section
        if ($content -notmatch 'fts-filter-section') {
            return
        }
        
        # 查找所有 fts-filter-section 的位置
        $filterMatches = [regex]::Matches($content, '<div class="fts-filter-section">')
        
        foreach ($match in $filterMatches) {
            $filterStart = $match.Index
            
            # 检查这个 filter-section 是否在 ContentCard 外面
            # 方法: 统计 filter-section 之前的 ContentCard 开闭标签
            $beforeContent = $content.Substring(0, $filterStart)
            $openCards = ([regex]::Matches($beforeContent, '<ContentCard[\s>]')).Count
            $closeCards = ([regex]::Matches($beforeContent, '</ContentCard>')).Count
            
            # 如果 openCards <= closeCards，说明在外面
            if ($openCards -le $closeCards) {
                # 找到 filter-section 的结束位置（匹配 </div>）
                $depth = 0
                $i = $filterStart
                $foundEnd = $false
                
                while ($i -lt $content.Length -and -not $foundEnd) {
                    if ($content[$i] -eq '<' -and $i+1 -lt $content.Length) {
                        if ($content.Substring($i, [Math]::Min(27, $content.Length-$i)) -eq '<div class="fts-filter-section"') {
                            $depth++
                        } elseif ($i+5 -lt $content.Length -and $content.Substring($i,6) -eq '</div>') {
                            $depth--
                            if ($depth -eq 0) {
                                $filterEnd = $content.IndexOf('>', $i) + 1
                                $foundEnd = $true
                            }
                        }
                    }
                    $i++
                }
                
                if (-not $foundEnd) {
                    continue
                }
                
                # 提取 filter-section 块
                $filterBlock = $content.Substring($filterStart, $filterEnd - $filterStart)
                
                # 查找后面的 ContentCard
                $afterFilter = $content.Substring($filterEnd)
                $cardMatch = [regex]::Match($afterFilter, '<ContentCard[\s>]')
                
                if (-not $cardMatch.Success) {
                    continue
                }
                
                $cardPos = $filterEnd + $cardMatch.Index
                
                # 找到插入点（在 #actions 或 #header 的 </template> 之后）
                $afterCard = $content.Substring($cardPos, [Math]::Min(600, $content.Length - $cardPos))
                $insertOffset = 0
                
                # 查找最后一个 </template>
                $templateMatches = [regex]::Matches($afterCard, '</template>')
                if ($templateMatches.Count -gt 0) {
                    $lastTemplate = $templateMatches[$templateMatches.Count - 1]
                    # 跳过 </template> 后面的空白字符
                    $afterLastTemplate = $afterCard.Substring($lastTemplate.Index + $lastTemplate.Length)
                    $wsMatch = [regex]::Match($afterLastTemplate, '^\s*\n\s*')
                    if ($wsMatch.Success) {
                        $insertOffset = $lastTemplate.Index + $lastTemplate.Length + $wsMatch.Length
                    } else {
                        $insertOffset = $lastTemplate.Index + $lastTemplate.Length
                    }
                } else {
                    # 没有 template slots，尝试找到 <ContentCard...> 结束标签后
                    $tagMatch = [regex]::Match($afterCard, '<ContentCard[^>]*>')
                    if ($tagMatch.Success) {
                        $insertOffset = $tagMatch.Index + $tagMatch.Length
                    }
                }
                
                if ($insertOffset -eq 0) {
                    continue
                }
                
                $insertPos = $cardPos + $insertOffset
                
                # 构建新内容
                $beforeFilter = $content.Substring(0, $filterStart).TrimEnd()
                $between = $content.Substring($filterEnd, insertPos - $filterEnd)
                $afterInsert = $content.Substring($insertPos)
                
                # 格式化 filter 块
                $trimmedFilter = $filterBlock.Trim()
                $indentedFilter = "      `n" + (($trimmedFilter -split "`n") | ForEach-Object { "        $_" }) -join "`n"
                
                $newContent = "$beforeFilter`n`n$between`n$indentedFilter`n$afterInsert"
                
                [System.IO.File]::WriteAllText($FilePath, $newContent, [System.Text.Encoding]::UTF8)
                $script:fixedCount++
                $relPath = $FilePath.Replace($viewsDir, "").Replace("\", "/")
                Write-Host "[FIXED] $relPath" -ForegroundColor Green
                return
            }
        }
        
        $script:skippedCount++
    } catch {
        $script:errorFiles += @{ File = $FilePath; Error = $_.Exception.Message }
    }
}

# 遍历所有 .vue 文件
Get-ChildItem -Path $viewsDir -Recurse -Filter "*.vue" | ForEach-Object {
    Fix-File-If-Needed -FilePath $_.FullName
}

Write-Host "`n========== 修复完成 ==========" -ForegroundColor Cyan
Write-Host "已修复: $fixedCount"
Write-Host "跳过(无需修复/已在内部): $skippedCount"

if ($errorFiles.Count -gt 0) {
    Write-Host "`n错误文件:" -ForegroundColor Red
    $errorFiles | ForEach-Object {
        Write-Host "  $($_.File): $($_.Error)" -ForegroundColor Red
    }
}
