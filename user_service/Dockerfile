FROM eclipse-temurin:17-jre
WORKDIR /app
COPY user_service/target/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75.0"
EXPOSE 8089
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
