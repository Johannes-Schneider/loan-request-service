FROM maven:3.9.6-eclipse-temurin-17-focal AS build
WORKDIR /app

COPY . /app
RUN mvn clean package -P postgres --no-transfer-progress

FROM eclipse-temurin:17.0.10_7-jdk
COPY --from=build /app/target/loan-request-service-*.jar /app.jar
EXPOSE 8080

ENTRYPOINT ["java","-jar","/app.jar"]
