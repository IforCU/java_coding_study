FROM maven:3.10.0-eclipse-temurin-17-noble AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY modules ./modules
RUN mvn -B -ntp -pl modules/01-java-core -am verify

FROM eclipse-temurin:17-jre-noble
WORKDIR /app
COPY --from=build /workspace/modules/01-java-core/target/java-core.jar /app/java-core.jar
CMD ["java", "-cp", "/app/java-core.jar", "kr.banklearning.core.BankDemo"]
