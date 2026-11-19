# Build stage: compile the agent and collect its dependencies.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B compile dependency:copy-dependencies -DoutputDirectory=target/dependency

# Run stage: the question is the container's command, for example
#   docker compose run --rm agent "Which products cost less than 50 EUR?"
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/classes ./classes
COPY --from=build /build/target/dependency ./lib
ENTRYPOINT ["java", "-cp", "classes:lib/*", "dev.rabauer.live_coding.wanaku.agent.ShopAgent"]
