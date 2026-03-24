# Usamos la imagen oficial y ligera de Java 21
FROM eclipse-temurin:21-jre-alpine

# Creamos la carpeta de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el .jar generado por Maven
COPY target/usuarios-service-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto
EXPOSE 8081

# Ejecutamos la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]