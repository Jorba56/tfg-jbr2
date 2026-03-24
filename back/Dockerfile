# imagen oficial de Java 21 súper ligera (alpine)
FROM eclipse-temurin:21-jdk-alpine

# carpeta de trabajo dentro del contenedor llamada /app
WORKDIR /app

# copiamos el archivo .jar  al contenedor
COPY target/sprintDef-0.0.1-SNAPSHOT.jar app.jar

# nuestra API se comunica por el puerto 8080
EXPOSE 8080

# orden exacta que ejecutará Docker para arrancar
ENTRYPOINT ["java", "-jar", "app.jar"]