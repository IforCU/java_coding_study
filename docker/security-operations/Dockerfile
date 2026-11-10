FROM maven:3.10.0-eclipse-temurin-17-noble AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY modules ./modules
COPY docker/health /health-src
RUN mvn -B -ntp -pl modules/05-security-operations -am verify
RUN javac -d /health /health-src/HttpCheck.java
FROM tomcat:11.0.27-jdk17-temurin-noble
COPY --from=build /workspace/modules/05-security-operations/target/security-operations.war /usr/local/tomcat/webapps/ROOT.war
COPY --from=build /health /opt/health
HEALTHCHECK --interval=10s --timeout=10s --start-period=30s --retries=6 CMD ["java", "-cp", "/opt/health", "HttpCheck", "http://127.0.0.1:8080/health"]
CMD ["catalina.sh", "run"]
