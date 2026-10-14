FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S monitoring && adduser -S monitoring -G monitoring
COPY --from=build /app/target/*.jar app.jar
RUN chown monitoring:monitoring app.jar
USER monitoring
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -Xms128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
