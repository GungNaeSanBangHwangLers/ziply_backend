# Dockerfile 예시
FROM eclipse-temurin:17-jdk-focal
WORKDIR /app

# JAR 파일 복사
COPY build/libs/*.jar app.jar

# JSON 리소스 파일들을 컨테이너 내부의 /app/resources 폴더로 복사
# (프로젝트 구조에 따라 경로를 조절하세요. 보통 src/main/resources에 있다면 아래와 같습니다)
COPY src/main/resources/seoulBusPosition.json /app/resources/seoulBusPosition.json
COPY src/main/resources/seoulBusCount.json /app/resources/seoulBusCount.json

ENTRYPOINT ["java", "-jar", "app.jar"]