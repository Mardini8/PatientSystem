FROM eclipse-temurin:17-jre-alpine

EXPOSE 8082

COPY target/clinical-service-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java","-jar","/app.jar"]