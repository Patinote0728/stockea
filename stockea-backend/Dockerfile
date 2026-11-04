FROM eclipse-temurin:26-jdk AS compilacion

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:26-jre

WORKDIR /app
COPY --from=compilacion --chown=10001:10001 /app/target/stockea-backend-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar"]
