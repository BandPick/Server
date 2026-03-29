FROM gradle:8.14-jdk21-jammy AS build
USER gradle
WORKDIR /home/gradle/project

COPY --chown=gradle:gradle settings.gradle build.gradle ./
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle src ./src

RUN gradle bootJar --no-daemon --no-build-cache

RUN cp "$(ls build/libs/*-SNAPSHOT.jar | grep -v plain | head -n1)" /home/gradle/project/application.jar

# Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /home/gradle/project/application.jar ./application.jar

ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/application.jar --server.port=${PORT:-8080}"]
