# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn ./.mvn
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests -s .mvn/local-settings.xml

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar", "--spring.profiles.active=prod"]
