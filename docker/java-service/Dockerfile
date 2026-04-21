FROM maven:3.9.9-eclipse-temurin-17 AS builder

ARG MODULE_NAME
WORKDIR /workspace

COPY . .
RUN mvn -pl ${MODULE_NAME} -am clean package -DskipTests && \
    cp /workspace/${MODULE_NAME}/target/*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /workspace/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
