# EntityKart – Local Startup Script
# Updated: 2026-06-26 — uses common-services (merged gateway + eureka + notification)
# Launches core microservices locally, connecting directly to Aiven MySQL.

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   STARTING ENTITYKART LOCAL MICROSERVICES        " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Load Environment Variables from .env
if (Test-Path .env) {
    Write-Host "Loading environment variables from .env..." -ForegroundColor Green
    Get-Content .env | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $key, $val = $line -split '=', 2
            if ($key -and $val) {
                [System.Environment]::SetEnvironmentVariable($key.Trim(), $val.Trim())
            }
        }
    }
} else {
    Write-Error ".env file not found! Please create a .env file."
    Exit
}

# 2. Set shared Eureka URL pointing at common-services
$env:EUREKA_CLIENT_SERVICEURL_DEFAULTZONE = "http://localhost:9900/eureka/"

# Ensure JVM optimization params
$JVM_OPTS = "-Xmx64m -Xms32m -XX:MaxMetaspaceSize=75m -XX:+UseSerialGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dspring.main.lazy-initialization=true -Dspring.devtools.restart.enabled=false"

# 3. Start common-services on Port 9900
#    (Eureka Server + Spring Cloud Gateway + Notifications + Email)
Write-Host "Starting common-services on port 9900 (Eureka + Gateway + Notifications)..." -ForegroundColor Yellow
$env:SERVER_PORT  = "9900"
$env:DB_NAME      = "notification_service"
Start-Process java -ArgumentList "$JVM_OPTS -jar common-services/build/libs/common-services.jar" -WindowStyle Minimized

Write-Host "Waiting 20s for Eureka to start..." -ForegroundColor DarkYellow
Start-Sleep -Seconds 20

# 4. Start User Service on Port 9902
Write-Host "Starting User Service on port 9902 (database: user_service)..." -ForegroundColor Yellow
$env:SERVER_PORT = "9902"
$env:DB_NAME     = "user_service"
Start-Process java -ArgumentList "$JVM_OPTS -jar user-service/build/libs/user-service.jar" -WindowStyle Minimized

Start-Sleep -Seconds 5

# 5. Start Product Service on Port 9903
Write-Host "Starting Product Service on port 9903 (database: product_service)..." -ForegroundColor Yellow
$env:SERVER_PORT = "9903"
$env:DB_NAME     = "product_service"
Start-Process java -ArgumentList "$JVM_OPTS -jar product-service/build/libs/product-service.jar" -WindowStyle Minimized

Start-Sleep -Seconds 5

# 6. Start Frontend on Port 3000
Write-Host "Starting Frontend HTTP server on port 3000..." -ForegroundColor Yellow
Start-Process npm -ArgumentList "start" -WorkingDirectory frontend -WindowStyle Minimized

Write-Host "==================================================" -ForegroundColor Green
Write-Host " All services started in minimized windows!       " -ForegroundColor Green
Write-Host " - Eureka Dashboard   : http://localhost:9900     " -ForegroundColor Green
Write-Host " - API Gateway        : http://localhost:9900/api/" -ForegroundColor Green
Write-Host " - Notifications REST : http://localhost:9900/api/admin/notifications" -ForegroundColor Green
Write-Host " - Frontend Portal    : http://localhost:3000     " -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
