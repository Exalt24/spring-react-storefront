# Two stages so the runtime image carries a JRE and a jar, not Maven and a
# ~/.m2 cache. Dependencies are resolved in their own layer first, so a source
# change does not re-download the world on every deploy.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests are skipped HERE on purpose: they need a Docker daemon for
# Testcontainers, which a build container does not have. The suite is the gate
# before pushing, not during the image build.
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Never run as root in a container that faces the internet.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 8080

# Render supplies PORT. Spring reads SERVER_PORT, so bridge the two rather than
# assuming 8080 survives.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
