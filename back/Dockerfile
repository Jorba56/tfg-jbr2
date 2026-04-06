# ETAPA 1: Construcción (Build)
# Usamos Maven con JDK 21 para compilar el proyecto
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Definimos el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos los archivos de configuración de Maven primero para aprovechar la caché
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY eureka-server/pom.xml eureka-server/
COPY incidencias-service/pom.xml incidencias-service/
# Añade aquí otros submódulos si los tienes

# Copiamos el código fuente
COPY . .

# Compilamos y generamos los archivos .jar saltando los tests
RUN mvn clean package -DskipTests

# ETAPA 2: Ejecución (Run)
# Usamos JRE 21 sobre Alpine para que la imagen sea muy pequeña
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos el archivo .jar generado desde la etapa de construcción
# IMPORTANTE: Verifica que el nombre coincida con el <artifactId> de tu pom.xml principal
# Según tus archivos anteriores, el nombre esperado es sprintDef-0.0.1-SNAPSHOT.jar
COPY --from=build /app/target/sprintDef-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto que usa tu aplicación (normalmente 8080)
EXPOSE 8080

# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]