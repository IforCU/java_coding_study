FROM maven:3.10.0-eclipse-temurin-17-noble AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY modules ./modules
COPY docker/health /health-src
RUN mvn -B -ntp -pl modules/06-spring-boot -am verify
RUN javac -d /health /health-src/HttpCheck.java
FROM eclipse-temurin:17-jre-noble
WORKDIR /app
COPY --from=build /workspace/modules/06-spring-boot/target/spring-boot-api.jar /app/app.jar
COPY --from=build /health /opt/health
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=10s --start-period=30s --retries=6 CMD ["java", "-cp", "/opt/health", "HttpCheck", "http://127.0.0.1:8080/health"]
CMD ["java", "-XX:MaxRAMPercentage=65.0", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
