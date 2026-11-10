# Script hỗ trợ khởi chạy Backend với các biến môi trường từ .env
param(
    [string]$Password = ""
)

$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line.Split("=", 2)
            if ($parts.Length -eq 2) {
                $name = $parts[0].Trim()
                $value = $parts[1].Trim()
                [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }
}

if ($Password) {
    $env:SUPABASE_DB_PASSWORD = $Password
}

Write-Host "Connecting to Supabase Database..." -ForegroundColor Cyan
Write-Host "URL: $env:SUPABASE_DB_URL"
Write-Host "User: $env:SUPABASE_DB_USERNAME"

if (-not $env:SUPABASE_DB_PASSWORD -or $env:SUPABASE_DB_PASSWORD -eq "[YOUR-PASSWORD]") {
    Write-Warning "Vui long cap nhat SUPABASE_DB_PASSWORD trong file .env hoac truyen qua tham so -Password <mat_khau>"
}

# Chạy backend qua Maven nếu có hoặc thông báo
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    mvn spring-boot:run
} elseif (Test-Path ".\mvnw.cmd") {
    .\mvnw.cmd spring-boot:run
} else {
    Write-Host "Khong tim thay lenh 'mvn'. Ban co the mo du an bang VS Code / IntelliJ IDEA va chay ung dung StoreBeApplication." -ForegroundColor Yellow
}

