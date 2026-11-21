FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/multi-tenant-task-management-system-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
