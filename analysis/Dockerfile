# 기존 openjdk 대신 eclipse-temurin 사용
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# JAR 파일 복사 (실행 가능한 boot JAR만 복사)
COPY build/libs/*.jar app.jar

# 리소스 파일 복사
COPY src/main/resources/seoulBusPosition.json.gz /app/resources/seoulBusPosition.json.gz
COPY src/main/resources/seoulBusCount.json /app/resources/seoulBusCount.json


# 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]