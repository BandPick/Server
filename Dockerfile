# Build (Gradle 이미지 사용 — gradle-wrapper.jar 미커밋 환경에서도 동작)
FROM gradle:8.14-jdk21-jammy AS build
WORKDIR /workspace

COPY --chown=gradle:gradle settings.gradle build.gradle ./
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle src ./src

USER gradle
RUN gradle bootJar --no-daemon --no-build-cache

# Fat JAR only (exclude *-plain.jar)
RUN cp "$(ls build/libs/*-SNAPSHOT.jar | grep -v plain | head -n1)" /workspace/application.jar

# Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /workspace/application.jar ./application.jar

ENV JAVA_OPTS=""
EXPOSE 8080

# Render는 PORT 지정; 로컬은 기본 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/application.jar --server.port=${PORT:-8080}"]
