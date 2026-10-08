ARG CONTRACTS_SOURCE=published

FROM eclipse-temurin:21-jdk AS source
WORKDIR /app
COPY . .

FROM source AS contracts-published

FROM source AS contracts-local
COPY --from=payment-contracts /pom.xml /contracts/pom.xml
COPY --from=payment-contracts /src /contracts/src
RUN ./mvnw -B -ntp -f /contracts/pom.xml install -DskipTests

FROM contracts-${CONTRACTS_SOURCE} AS build
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    if [ -f /run/secrets/github_actor ]; then export GITHUB_ACTOR="$(cat /run/secrets/github_actor)"; fi && \
    if [ -f /run/secrets/github_token ]; then export GITHUB_TOKEN="$(cat /run/secrets/github_token)"; fi && \
    ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
