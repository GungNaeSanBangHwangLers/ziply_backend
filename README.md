# Ziply Backend

마이크로서비스 아키텍처 기반의 주거 탐색 플랫폼 백엔드 시스템입니다.

## 프로젝트 구조

```
ziply-backend/
├── auth/      # 인증 서비스 (포트 8081)
├── user/      # 사용자 서비스 (포트 8080)
├── review/    # 리뷰/탐색 카드 서비스 (포트 8082)
└── gateway/   # API Gateway (포트 8000)
```

## 기술 스택

- **언어**: Java 17
- **프레임워크**: Spring Boot 3.3.5
- **빌드 도구**: Gradle
- **데이터베이스**: MySQL
- **인증**: JWT (JSON Web Token)
- **API 문서**: Swagger/OpenAPI
- **테스트**: JUnit 5, Mockito, AssertJ

## 아키텍처

### 마이크로서비스 구조

각 서비스는 독립적으로 배포 가능하며, 다음과 같은 책임을 가집니다:

- **auth**: Google OAuth 인증 및 JWT 토큰 발급
- **user**: 사용자 정보 관리
- **review**: 주거 탐색 카드 및 기점 관리
- **gateway**: API 라우팅 및 요청 분산

### 레이어드 아키텍처

각 서비스는 다음 레이어로 구성됩니다:

```
Controller → Service → Repository → Domain
```

## 실행 방법

### 사전 요구사항

- Java 17 이상
- MySQL 8.0 이상
- Gradle 7.x 이상

### 데이터베이스 설정

각 서비스별로 독립적인 데이터베이스를 사용합니다:

- `ziply_auth`
- `ziply_user`
- `ziply_review`

### 실행 순서

1. MySQL 데이터베이스 생성 및 설정
2. 각 서비스의 `application.yml`에서 데이터베이스 연결 정보 수정
3. 서비스 실행:

```bash
# User 서비스
cd user
./gradlew bootRun

# Auth 서비스
cd auth
./gradlew bootRun

# Review 서비스
cd review
./gradlew bootRun

# Gateway
cd gateway
./gradlew bootRun
```

## API 엔드포인트

### Auth Service (8081)

- `POST /api/v1/auth/google` - Google OAuth 로그인

### User Service (8080)

- `POST /api/v1/users` - 사용자 생성 (AUTH 내부 호출용)
- `GET /api/v1/users/me` - 내 정보 조회 (JWT 필요)
- `GET /api/v1/users/name` - 사용자 이름 조회 (JWT 필요)
- `PATCH /api/v1/users/me` - 사용자 정보 수정 (JWT 필요)

### Review Service (8082)

- `POST /api/v1/review/card` - 탐색 카드 생성 (JWT 필요)

### Gateway (8000)

모든 요청은 Gateway를 통해 라우팅됩니다.

## 인증 플로우

1. 클라이언트가 Google Sign-In SDK로 로그인
2. 획득한 ID 토큰을 Auth 서비스에 전달
3. Auth 서비스가 Google 토큰 검증
4. User 서비스에 사용자 정보 동기화 요청
5. JWT Access Token 및 Refresh Token 발급
6. 이후 요청에 JWT 토큰을 Bearer 토큰으로 포함

## 테스트

### 단위 테스트 실행

```bash
./gradlew test
```

### 통합 테스트 실행

통합 테스트는 H2 인메모리 데이터베이스를 사용합니다:

```bash
./gradlew test
```

### 테스트 커버리지

JaCoCo를 사용하여 테스트 커버리지를 측정합니다:

```bash
./gradlew jacocoTestReport
```

## 개발 가이드

### 코드 스타일

- Java 코딩 컨벤션 준수
- Lombok 사용으로 보일러플레이트 코드 최소화
- DTO는 Record 또는 클래스 사용

### 예외 처리

- 각 모듈별 `GlobalExceptionHandler`를 통한 일관된 에러 응답
- 커스텀 예외 클래스 사용 (`UserException`, `ReviewException`, `AuthException`)

### 로깅

- SLF4J + Logback 사용
- 로그 레벨: DEBUG, INFO, WARN, ERROR
- 로그 형식: `[SERVICE_NAME] 메시지`

## 배포

### 환경 변수

중요한 설정값은 환경 변수로 관리합니다:

- `JWT_SECRET`: JWT 서명 키
- `GOOGLE_CLIENT_ID`: Google OAuth 클라이언트 ID
- 데이터베이스 연결 정보

### Docker

각 서비스는 독립적으로 Docker 이미지로 빌드 가능합니다.

## 라이선스

이 프로젝트는 내부 사용을 위한 것입니다.


