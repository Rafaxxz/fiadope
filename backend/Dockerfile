# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ---- runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 fiadope && mkdir -p /app/uploads && chown fiadope /app/uploads
COPY --from=build /app/target/fiadope-api-*.jar app.jar
USER fiadope
ENV UPLOADS_DIR=/app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
