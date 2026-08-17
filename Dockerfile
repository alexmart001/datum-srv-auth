# Etapa 1: build do jar com Maven, usando o wrapper do próprio projeto -
# não precisa de Maven instalado no host nem de "mvn package" manual
# antes do build, então "docker compose up" sozinho já é suficiente.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B -DskipTests package

# Etapa 2: imagem final, só com o jar (sem Maven/JDK de build, cache de
# dependências etc.)
FROM eclipse-temurin:21-jdk
COPY --from=build /workspace/target/*.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
