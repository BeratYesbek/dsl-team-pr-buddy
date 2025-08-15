FROM --platform=linux/amd64 gradle:8.10-jdk17 AS builder

RUN mkdir -p /app/source
COPY . /app/source

WORKDIR /app/source

RUN gradle clean build -x test

FROM --platform=linux/amd64 eclipse-temurin:17-jre

COPY --from=builder /app/source/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]