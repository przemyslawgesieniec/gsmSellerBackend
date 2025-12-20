# ---------- BUILD STAGE ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests


# ---------- RUNTIME STAGE ----------
FROM amazoncorretto:21

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]