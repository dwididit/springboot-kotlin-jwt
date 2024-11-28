FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Create directory for logs
RUN mkdir -p /app/logs

# Set environment variables
ENV SPRING_DATA_MONGODB_HOST=mongodb
ENV SPRING_DATA_MONGODB_PORT=27017
ENV SPRING_DATA_MONGODB_DATABASE=mongodb_crud
ENV SPRING_DATA_MONGODB_USERNAME=mongodb
ENV SPRING_DATA_MONGODB_PASSWORD=mongodb
ENV SERVER_PORT=9090

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]