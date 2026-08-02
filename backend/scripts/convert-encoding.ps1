$content = Get-Content -Path 'P:\my-new-project\backend\scripts\test-api-4modules.ps1' -Raw -Encoding UTF8
$utf8WithBom = New-Object System.Text.UTF8Encoding $true
[System.IO.File]::WriteAllText('P:\my-new-project\backend\scripts\test-api-4modules.ps1', $content, $utf8WithBom)
Write-Host 'Encoding converted to UTF-8 with BOM'
