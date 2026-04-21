FROM eclipse-temurin:17-jre

ARG SENTINEL_VERSION=1.8.8
ADD https://github.com/alibaba/Sentinel/releases/download/${SENTINEL_VERSION}/sentinel-dashboard-${SENTINEL_VERSION}.jar /app/sentinel-dashboard.jar

WORKDIR /app
EXPOSE 8858

ENTRYPOINT ["java", "-Dserver.port=8858", "-Dcsp.sentinel.dashboard.server=sentinel-dashboard:8858", "-Dproject.name=sentinel-dashboard", "-jar", "/app/sentinel-dashboard.jar"]
