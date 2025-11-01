# Använd Java 17 JRE (Runtime Environment) som basimage
# eclipse-temurin är en stabil och populär distribution. Alpine ger en liten image.
FROM eclipse-temurin:17-jre-alpine

# Exponera port 8080
EXPOSE 8080

# Kopiera den *färdigbyggda* JAR-filen från din lokala 'target'-mapp till 'app.jar' inuti containern
COPY target/PatientSystem-0.0.1-SNAPSHOT.jar app.jar

# Starta applikationen
ENTRYPOINT ["java","-jar","/app.jar"]