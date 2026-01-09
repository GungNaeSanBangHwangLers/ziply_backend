# 기존 openjdk 대신 eclipse-temurin 사용
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# JAR 파일 복사
COPY build/libs/*-SNAPSHOT.jar app.jar

# 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]