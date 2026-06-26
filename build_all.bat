@echo off
REM EntityKart – Build All Services
REM Updated 2026-06-26: common-services replaces api-gateway + discovery-server + notification-service
set services=common-services user-service product-service cart-service order-service payment-service wishlist-service review-service return-service

for %%s in (%services%) do (
    echo ===================================================
    echo BUILDING SERVICE: %%s
    echo ===================================================
    cd %%s
    call .\gradlew bootJar
    if errorlevel 1 (
        echo ERROR: Build failed for %%s
        cd ..
        exit /b 1
    )
    cd ..
)

echo ===================================================
echo ALL SERVICES BUILT SUCCESSFULLY!
echo ===================================================
