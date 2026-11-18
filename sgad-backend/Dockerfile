# ───────── Etapa 1: compilar con Maven (imagen grande, se descarta al final) ─────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Primero solo el pom.xml: Docker guarda en caché las dependencias y no las vuelve a bajar si solo cambia el código
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Corre las pruebas unitarias (no necesitan MongoDB); si alguna falla, la imagen no se construye
RUN mvn -B -q package

# ───────── Etapa 2: imagen final, solo Java + el .jar (pequeña) ─────────
FROM eclipse-temurin:17-jre
WORKDIR /app
# No ejecutar como root dentro del contenedor
RUN useradd --system --no-create-home sgad
COPY --from=build /app/target/sgad-backend-1.0.0.jar app.jar
USER sgad
EXPOSE 8080
# Usa solo parte de la RAM del contenedor (los servidores gratuitos tienen poca)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
