# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    if [ -f /run/secrets/github_actor ]; then export GITHUB_ACTOR="$(cat /run/secrets/github_actor)"; fi && \
    if [ -f /run/secrets/github_token ]; then export GITHUB_TOKEN="$(cat /run/secrets/github_token)"; fi && \
    ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/transaction-checker-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]