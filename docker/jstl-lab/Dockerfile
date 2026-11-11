FROM maven:3.10.0-eclipse-temurin-17-noble AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY modules ./modules
COPY exercises/jstl-bank-lab ./exercises/jstl-bank-lab
COPY exercises/starter/phase-02/jstl ./exercises/starter/phase-02/jstl
RUN mvn -B -ntp -Pjstl-lab -pl exercises/jstl-bank-lab -am -DskipTests package

FROM tomcat:10.1.61-jdk17-temurin-noble
COPY --from=build /workspace/exercises/jstl-bank-lab/target/jstl-bank-lab-starter.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
