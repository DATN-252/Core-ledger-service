Write-Host "🔧 Building Core Ledger Service..." -ForegroundColor Cyan
Write-Host ""

# Make sure we're in the right directory
$expectedPath = "d:\Code\DoAn\DATN\core-ledger-service"
$currentPath = Get-Location

if ($currentPath.Path -ne $expectedPath) {
    Write-Host "Changing to correct directory..." -ForegroundColor Yellow
    Set-Location $expectedPath
}

Write-Host "Current directory: $(Get-Location)" -ForegroundColor Gray
Write-Host ""

# Clean build
Write-Host "Step 1: Cleaning previous build..." -ForegroundColor Yellow
.\gradlew.bat clean

Write-Host ""
Write-Host "Step 2: Building project (this may take a minute)..." -ForegroundColor Yellow  
.\gradlew.bat build --refresh-dependencies

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Build successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Now starting service..." -ForegroundColor Cyan
    Write-Host ""
    .\gradlew.bat bootRun
} else {
    Write-Host ""
    Write-Host "❌ Build failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Try using IntelliJ IDEA instead:" -ForegroundColor Yellow
    Write-Host "1. Open IntelliJ IDEA" -ForegroundColor White
    Write-Host "2. Open project: $expectedPath" -ForegroundColor White
    Write-Host "3. Run LedgerServiceApplication.java" -ForegroundColor White
}
