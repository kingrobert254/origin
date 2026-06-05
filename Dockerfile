```dockerfile
# Langkah 1: Build aplikasi menggunakan Maven dan OpenJDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Langkah 2: Jalankan aplikasi menggunakan OpenJDK 21 yang ringan
FROM eclipse-temurin:21-jre-jammy
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]