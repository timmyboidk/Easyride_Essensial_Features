# Build stage
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
# Copy pom files first for better caching
COPY pom.xml .
COPY admin_service/pom.xml admin_service/
# Add other modules pom to allow root build
COPY analytics_service/pom.xml analytics_service/
COPY location_service/pom.xml location_service/
COPY matching_service/pom.xml matching_service/
COPY notification_service/pom.xml notification_service/
COPY order_service/pom.xml order_service/
COPY payment_service/pom.xml payment_service/
COPY review_service/pom.xml review_service/
COPY user_service/pom.xml user_service/

# Download dependencies
RUN mvn dependency:go-offline -B -pl admin_service -am

# Copy source and build
COPY admin_service/src admin_service/src
RUN mvn package -DskipTests -pl admin_service -am

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/admin_service/target/*.jar app.jar

# Optimizations for Docker 4.55+ and JVM
ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75.0"

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
