# ---------- BUILD STAGE ----------
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 1️⃣ tylko pom.xml
COPY pom.xml .

# 2️⃣ cache zależności
RUN mvn -B dependency:go-offline

# 3️⃣ dopiero źródła
COPY src ./src

# 4️⃣ build
RUN mvn -B package -DskipTests


# ---------- RUNTIME STAGE ----------
FROM amazoncorretto:21

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]