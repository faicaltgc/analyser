
# Basis-Image mit Java
FROM openjdk:23-slim

# Arbeitsverzeichnis setzen
WORKDIR /app
# Copy files with values from .env
#COPY src/main/resources/.env /app/.env
COPY src/main/resources/application.properties /app/application.properties
# Kopiere das gebaute JAR-File ins Image
COPY target/analyser-0.0.1-SNAPSHOT.jar app.jar

# Port freigeben
EXPOSE 8080

# Startbefehl für die Anwendung
ENTRYPOINT ["java", "-jar", "app.jar"]