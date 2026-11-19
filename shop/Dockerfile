# Build stage: Maven compiles the Quarkus app.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

# Run stage: only the JRE and the built app.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/quarkus-app/ ./
EXPOSE 9090
CMD ["java", "-jar", "quarkus-run.jar"]
