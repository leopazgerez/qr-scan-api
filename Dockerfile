# Primera etapa: Construcción del JAR
FROM maven:3.8.5-openjdk-17 AS builder

# Establecer directorio de trabajo
WORKDIR /app

# Copiar los archivos del proyecto
COPY . .

# Compilar la aplicación
RUN mvn clean package -DskipTests
# Usar una imagen base de OpenJDK 17
FROM openjdk:17-jdk-alpine

# Establecer un directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el archivo JAR generado al contenedor
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto en el que se ejecutará la aplicación
EXPOSE 8091

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]