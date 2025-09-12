
# Basis-Image mit Java
FROM eclipse-temurin:17-jdk-alpine

# Arbeitsverzeichnis setzen
WORKDIR /app
# Copy files with values from .env
#COPY src/main/resources/.env /app/.env
# Kopiere das gebaute JAR-File ins Image
COPY target/analyser-0.0.1-SNAPSHOT.jar app.jar

# Port freigeben
EXPOSE 8080
ENV MONGO_DATABASE=$MONGO_DATABASE
ENV MONGO_USER=$MONGO_USER
ENV MONGO_PASSWORD=$MONGO_PASSWORD
ENV MONGO_CLUSTER=$MONGO_CLUSTER
ENV SECRET_KEY=$SECRET_KEY

# Startbefehl für die Anwendung
CMD ["java", "-jar", "app.jar"]