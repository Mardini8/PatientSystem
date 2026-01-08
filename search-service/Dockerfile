FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

EXPOSE 8084

# Copy the Quarkus runner jar
COPY target/quarkus-app/lib/ /app/lib/
COPY target/quarkus-app/*.jar /app/
COPY target/quarkus-app/app/ /app/app/
COPY target/quarkus-app/quarkus/ /app/quarkus/

# Set production profile
ENV QUARKUS_PROFILE=prod

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]