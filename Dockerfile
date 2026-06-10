# Multi-stage build for repcheck-utils
# Stage 1: Build with sbt-assembly
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./sbt repcheckutils/assembly

# Stage 2: Runtime with Google Distroless
FROM gcr.io/distroless/java21-debian12
WORKDIR /app
COPY --from=build /app/repcheck-utils/target/scala-3.7.3/*-assembly-*.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
