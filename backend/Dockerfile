FROM eclipse-temurin:17-jre-alpine

EXPOSE 8080

COPY target/PatientSystem-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]