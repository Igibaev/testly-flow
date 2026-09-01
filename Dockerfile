FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# Production frontend bundle needs network: the plugin downloads Node into ~/.vaadin
# and runs npm. Maven go-offline cannot cache that, so this is a single package step.
RUN mvn -B -Pproduction package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
