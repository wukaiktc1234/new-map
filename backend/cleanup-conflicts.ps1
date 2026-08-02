# Batch cleanup script for Bean conflicts
# Identify and delete old ServiceImpl files in root that conflict with sub-packages

$baseDir = "p:\my-new-project\backend\src\main\java\com\example\demo"
$conflicts = @()
$deleted = @()

# Get all ServiceImpl in sub-packages (new code)
$subPackageServices = Get-ChildItem -Path "$baseDir\service" -Recurse -Filter "*ServiceImpl.java" |
    Where-Object { $_.FullName -notmatch "\\service\\impl\\" } |
    ForEach-Object { $_.Name }

# Get all ServiceImpl in root impl (old code)
$rootServices = Get-ChildItem -Path "$baseDir\service\impl" -Filter "*ServiceImpl.java" |
    ForEach-Object { $_.Name }

# Find conflicts (same name, different location)
foreach ($service in $rootServices) {
    if ($subPackageServices -contains $service) {
        $conflicts += $service
    }
}

Write-Host "Found $($conflicts.Count) potential Bean conflicts:"
$conflicts | ForEach-Object { Write-Host "  - $_" }

# Delete conflicting old files
foreach ($conflict in $conflicts) {
    $filePath = "$baseDir\service\impl\$conflict"
    if (Test-Path $filePath) {
        Remove-Item $filePath -Force
        $deleted += $conflict
        Write-Host "Deleted: $conflict"
    }
}

Write-Host "`nTotal deleted: $($deleted.Count) conflict files"
Write-Host "Deletion list:"
$deleted | ForEach-Object { Write-Host "  - service/impl/$_" }
