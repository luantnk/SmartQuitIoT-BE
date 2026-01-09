FROM maven:3.9.8-amazoncorretto-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

FROM amazoncorretto:21.0.4-al2023-headless
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

USER 1000
ENTRYPOINT ["java", "-jar", "app.jar"]