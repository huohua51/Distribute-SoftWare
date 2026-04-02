docker compose up -d --build
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\..'; mvn -pl seckill-inventory-service spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\..'; mvn -pl seckill-order-service spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\..'; mvn -pl seckill-payment-service spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\..'; mvn -pl seckill-seckill-service spring-boot:run"
