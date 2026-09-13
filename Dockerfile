FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode -DskipTests package

FROM eclipse-temurin:17-jre-noble

WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
