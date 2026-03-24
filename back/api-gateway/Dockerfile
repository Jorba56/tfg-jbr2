# Usamos la imagen oficial de Java 21
FROM eclipse-temurin:21-jre-alpine

# Creamos la carpeta de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el .jar generado por Maven
COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto del Gateway (el 8080)
EXPOSE 8080

# Ejecutamos la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]