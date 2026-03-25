# Stage 1: Build the application using Maven
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .

# Retry dependency resolution to handle transient DNS/network issues in Docker build environments.
RUN for i in 1 2 3 4 5; do \
			mvn -B -DskipTests -Dmaven.wagon.http.retryHandler.count=5 dependency:go-offline && break || sleep 10; \
		done

COPY src ./src
RUN for i in 1 2 3; do \
			mvn -B -DskipTests -Dmaven.wagon.http.retryHandler.count=5 clean package && break || sleep 10; \
		done

# Stage 2: Create the minimal runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
