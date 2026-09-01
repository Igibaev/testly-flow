FROM gradle:8.14.3-jdk17 AS build
WORKDIR /build
COPY settings.gradle build.gradle gradlew ./
COPY gradle ./gradle
COPY src ./src
# Production frontend bundle needs network: the plugin downloads Node into ~/.vaadin
# and runs npm. Gradle go-offline cannot cache that, so this is a single build step.
RUN ./gradlew -Pvaadin.productionMode=true bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
