docker compose up -d --build
mvn -pl seckill-seckill-service spring-boot:run -Dspring-boot.run.profiles=sharding
