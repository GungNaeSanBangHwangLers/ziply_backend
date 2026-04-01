# Ziply Backend

Ziply는 주거 탐색 경험을 지원하는 마이크로서비스 백엔드입니다.  
Google OAuth 기반 인증, 사용자 관리, 탐색 카드/집 정보 관리, 생활 인프라 분석 기능을 제공합니다.

## 프로젝트 개요

- 아키텍처: MSA (Service 별 독립 배포)
- 언어/런타임: Java 17
- 프레임워크: Spring Boot 3.3.5
- 빌드: Gradle
- DB: MySQL (테스트는 H2)
- 인증: JWT

## 서비스 구성

| 서비스 | 포트 | 역할 |
|---|---:|---|
| `gateway` | 8000 | API 라우팅 및 진입점 |
| `user` | 8080 | 사용자 정보 관리 |
| `auth` | 8081 | Google OAuth 검증, JWT 발급 |
| `review` | 8082 | 탐색 카드/집/측정 정보 관리 |
| `analysis` | (내부) | 거리/소음/안전도 분석 |

## 디렉터리 구조

```text
ziply-backend/
├── gateway/
├── user/
├── auth/
├── review/
└── analysis/
```

## 아키텍처

각 서비스는 기본적으로 아래 계층을 따릅니다.

```text
Controller -> Service -> Repository -> Domain
```

## 빠른 시작

### 1) 사전 준비

- Java 17+
- MySQL 8+
- Gradle Wrapper (`./gradlew`)

### 2) DB 생성

서비스별 DB를 생성합니다.

- `ziply_auth`
- `ziply_user`
- `ziply_review`
- `ziply_analysis` (사용 시)

### 3) 환경 설정

각 모듈의 `src/main/resources/application.yml`에서 아래 값을 환경에 맞게 수정합니다.

- DB 접속 정보
- JWT 시크릿
- 외부 API 키 (Google/Kakao/ODsay 등)
- 서비스 간 호출 URL

### 4) 서비스 실행

```bash
# gateway
cd gateway && ./gradlew bootRun

# user
cd user && ./gradlew bootRun

# auth
cd auth && ./gradlew bootRun

# review
cd review && ./gradlew bootRun

# analysis
cd analysis && ./gradlew bootRun
```

## 주요 API 예시

### Auth (`auth`)

- `POST /api/v1/auth/google` : Google 로그인 후 JWT 발급

### User (`user`)

- `POST /api/v1/users` : 사용자 생성 (내부 호출)
- `GET /api/v1/users/me` : 내 정보 조회
- `GET /api/v1/users/name` : 사용자 이름 조회
- `PATCH /api/v1/users/me` : 사용자 정보 수정

### Review (`review`)

- `POST /api/v1/review/card` : 탐색 카드 생성

> 실제 외부 호출은 `gateway`를 통해 라우팅하는 것을 권장합니다.

## 인증 플로우

1. 클라이언트가 Google Sign-In으로 ID 토큰 획득
2. ID 토큰을 `auth` 서비스로 전달
3. `auth`에서 토큰 검증 후 `user` 동기화
4. Access/Refresh JWT 발급
5. 이후 API 요청 시 `Authorization: Bearer <token>` 사용

## 테스트

루트에서 전체 테스트:

```bash
./gradlew :auth:test :user:test :review:test :gateway:test :analysis:test
```

모듈 단위 테스트:

```bash
./gradlew :user:test
./gradlew :review:test
```

테스트 커버리지 리포트:

```bash
./gradlew jacocoTestReport
```

## 개발 가이드

- Java 코딩 컨벤션 준수
- Lombok으로 보일러플레이트 최소화
- 모듈별 `GlobalExceptionHandler` 기반 일관된 예외 응답
- 로그 포맷: `[SERVICE_NAME] ...`

## 배포

- 각 서비스는 독립적으로 Docker 이미지화 가능
- 민감 정보는 환경 변수/시크릿으로 관리
  - `JWT_SECRET`
  - `GOOGLE_CLIENT_ID`
  - DB 접속 정보
  - 외부 API 키

## 라이선스

내부 사용 목적 프로젝트입니다.



