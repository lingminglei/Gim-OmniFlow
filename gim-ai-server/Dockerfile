FROM maven:3.9-eclipse-temurin-17 AS builder

ARG SERVICE_MODULE

WORKDIR /workspace

COPY . .

RUN mvn -B -pl "${SERVICE_MODULE}" -am -DskipTests package

FROM eclipse-temurin:17-jre

ARG SERVICE_MODULE

WORKDIR /app

COPY --from=builder /workspace/${SERVICE_MODULE}/target/${SERVICE_MODULE}-0.0.1-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
