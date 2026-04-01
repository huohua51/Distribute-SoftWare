FROM apache/shardingsphere-proxy:latest

USER root

RUN curl -fL --retry 3 --retry-delay 5 \
    -o /opt/shardingsphere-proxy/ext-lib/mysql-connector-j-8.4.0.jar \
    https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar
FROM apache/shardingsphere-proxy:latest

ADD https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar /opt/shardingsphere-proxy/ext-lib/mysql-connector-j-8.4.0.jar
