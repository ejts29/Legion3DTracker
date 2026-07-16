# Usar una imagen base de Java 21
FROM eclipse-temurin:21-jdk-alpine

# Definir el directorio de trabajo
WORKDIR /app

# Copiar archivos necesarios de la carpeta tracker a la raíz del contenedor
COPY tracker/pom.xml .
COPY tracker/.mvn .mvn
COPY tracker/mvnw .
# Dar permisos de ejecución al wrapper de Maven
RUN chmod +x mvnw

# Descargar dependencias
RUN ./mvnw dependency:go-offline

# Copiar el código fuente a la carpeta src del contenedor
COPY tracker/src ./src

# Construir el proyecto
RUN ./mvnw clean package -DskipTests

# Comando para ejecutar la aplicación
# El JAR se genera en la carpeta target tras la compilación
CMD ["java", "-jar", "target/*.jar"]