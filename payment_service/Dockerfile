# Build stage
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY payment_service/pom.xml payment_service/
COPY admin_service/pom.xml admin_service/
COPY analytics_service/pom.xml analytics_service/
COPY location_service/pom.xml location_service/
COPY matching_service/pom.xml matching_service/
COPY notification_service/pom.xml notification_service/
COPY order_service/pom.xml order_service/
COPY review_service/pom.xml review_service/
COPY user_service/pom.xml user_service/

RUN mvn dependency:go-offline -B -pl payment_service -am
COPY payment_service/src payment_service/src
RUN mvn package -DskipTests -pl payment_service -am

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/payment_service/target/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75.0"
EXPOSE 8087
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
