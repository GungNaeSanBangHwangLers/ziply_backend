# Ziply Backend

Ziply는 주거 탐색 경험을 지원하는 **이벤트 기반 마이크로서비스(Event-Driven MSA)** 백엔드입니다.  
Google OAuth 기반 인증, 사용자 관리, 탐색 카드/집 정보 관리 및 **Kafka**를 활용한 실시간 생활 인프라 분석 기능을 제공합니다.

## 1. 프로젝트 개요

- **아키텍처**: MSA (Service 별 독립 배포)
- **메시지 브로커**: Apache Kafka (비동기 분석 및 서비스 간 데이터 동기화)
- **언어/런타임**: Java 17
- **프레임워크**: Spring Boot 3.3.5
- **빌드**: Gradle
- **DB**: MySQL (Test: H2)
- **인증**: JWT (Google OAuth 2.0)

## 2. 서비스 구성

| 서비스 | 포트 | 역할 | 비고 |
|:--- |:---:|:--- |:--- |
| `gateway` | 8000 | API 라우팅 및 단일 진입점 | Spring Cloud Gateway |
| `auth` | 8081 | Google OAuth 검증 및 JWT 발급 | |
| `user` | 8080 | 사용자 프로필 및 계정 관리 | |
| `review` | 8082 | 탐색 카드/집 정보 관리 | **Kafka Producer** |
| `analysis` | 8083 | 거리/소음/안전도 인프라 분석 | **Kafka Consumer** |

## 3. 아키텍처 및 데이터 흐름

### 계층 구조
각 서비스는 표준 레이어드 아키텍처를 따릅니다.
`Controller` -> `Service` -> `Repository` -> `Domain`

### 이벤트 기반 분석 플로우 (Kafka)
1. **Event Publish**: `review` 서비스에서 탐색 카드가 생성/수정되면 위치 데이터를 Kafka Topic(`review-events`)으로 발행합니다.
2. **Event Consume**: `analysis` 서비스가 해당 이벤트를 구독하여 소음, 안전도, 주변 인프라 데이터를 비동기로 계산합니다.
3. **Data Sync**: 분석 결과가 완료되면 DB에 저장되며, 사용자는 지연 없이 분석된 데이터를 조회할 수 있습니다.

## 4. 디렉터리 구조

```text
ziply-backend/
├── gateway/      # API Gateway (Routing)
├── auth/         # 인증 및 인가 서비스 (OAuth2, JWT)
├── user/         # 사용자 관리 서비스
├── review/       # 리뷰 및 카드 관리 (Event Producer)
└── analysis/     # 인프라 분석 서비스 (Event Consumer)