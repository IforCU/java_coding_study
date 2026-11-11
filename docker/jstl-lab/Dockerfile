FROM maven:3.10.0-eclipse-temurin-17-noble AS build
ARG JSTL_PHASE=02
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY modules ./modules
COPY exercises/jstl-bank-lab ./exercises/jstl-bank-lab
COPY exercises/starter/phase-02/jstl ./exercises/starter/phase-02/jstl
COPY exercises/starter/phase-03/jstl ./exercises/starter/phase-03/jstl
COPY exercises/starter/phase-04/jstl ./exercises/starter/phase-04/jstl
COPY exercises/starter/phase-05/jstl ./exercises/starter/phase-05/jstl
COPY exercises/starter/phase-06/jstl ./exercises/starter/phase-06/jstl
RUN mvn -B -ntp -Pjstl-lab -pl exercises/jstl-bank-lab -am -Dlab.phase=${JSTL_PHASE} -DskipTests package && cp exercises/jstl-bank-lab/target/jstl-bank-lab-${JSTL_PHASE}-starter.war /workspace/lab.war

FROM tomcat:10.1.61-jdk17-temurin-noble
COPY --from=build /workspace/lab.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
