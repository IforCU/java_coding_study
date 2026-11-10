FROM maven:3.10.0-eclipse-temurin-17-noble AS sources
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY modules ./modules

FROM sources AS test
CMD ["mvn", "-B", "-ntp", "verify"]

FROM sources AS build
RUN mvn -B -ntp -pl modules/00-bootstrap -am verify

FROM tomcat:10.1.61-jdk17-temurin-noble AS runtime
COPY --from=build /workspace/modules/00-bootstrap/target/bootstrap.war /usr/local/tomcat/webapps/ROOT.war
COPY --from=build /workspace/modules/00-bootstrap/target/classes/kr/banklearning/bootstrap/HealthCheck.class /opt/health/kr/banklearning/bootstrap/HealthCheck.class
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=10s --start-period=30s --retries=6 \
  CMD ["java", "-cp", "/opt/health", "kr.banklearning.bootstrap.HealthCheck"]
CMD ["catalina.sh", "run"]
