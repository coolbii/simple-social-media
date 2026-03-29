FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY apps/api/gradlew ./gradlew
COPY apps/api/gradle ./gradle
COPY apps/api/build.gradle ./build.gradle
COPY apps/api/settings.gradle ./settings.gradle
COPY apps/api/src ./src

RUN chmod +x ./gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN useradd --create-home --uid 10001 appuser

WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
USER appuser

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
