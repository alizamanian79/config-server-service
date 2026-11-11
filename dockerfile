# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# اول فقط pom.xml تا لایه‌ی وابستگی‌ها کش بشه
COPY pom.xml .
RUN mvn -B dependency:go-offline

# بعد سورس کد
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# curl برای healthcheck
RUN apk add --no-cache curl \
    && addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/config-server-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

EXPOSE 8888

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8888/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]