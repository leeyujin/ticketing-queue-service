## 내용 소개
- 패스트캠퍼스 '[9개 프로젝트로 경험하는 대용량 트래픽 & 데이터 처리 초격차 패키지 Online.](https://fastcampus.co.kr/dev_online_traffic_data)' 수강 중 작성한 코드입니다.



# Ticketing Queue Service

## 개요

**Ticketing Queue Service**는 대규모 트래픽 상황에서 서버 부하를 분산시키고 공정한 접근을 보장하기 위한 대기열(Queue) 관리 시스템입니다. 

이 시스템은 다음과 같은 특징을 가집니다:

- **Reactive Programming**: Spring WebFlux를 활용한 비동기 논블로킹 처리
- **Redis 기반 대기열**: Redis Sorted Set을 활용한 효율적인 대기열 관리
- **자동 허용 처리**: 스케줄러를 통한 주기적인 사용자 허용 처리
- **토큰 기반 인증**: SHA-256 해시를 활용한 안전한 진입 토큰 생성
- **실시간 순번 조회**: 대기 중인 사용자의 현재 순번 실시간 조회

## 기술 스택

- **Java 17**
- **Spring Boot 3.4.2**
- **Spring WebFlux** (Reactive Web)
- **Spring Data Redis Reactive**
- **Redis** (Sorted Set 활용)
- **Thymeleaf** (템플릿 엔진)
- **Gradle** (빌드 도구)

## 프로젝트 구조

```
ticketing-queue-service/
├── src/
│   ├── main/
│   │   ├── java/com/ticketing/flow/
│   │   │   ├── controller/
│   │   │   │   ├── UserQueueController.java      # REST API 컨트롤러
│   │   │   │   └── WaitingRoomController.java    # 대기실 페이지 컨트롤러
│   │   │   ├── service/
│   │   │   │   ├── UserQueueService.java         # 대기열 비즈니스 로직
│   │   │   │   └── UserQueueScheduleService.java # 스케줄러 서비스
│   │   │   ├── dto/
│   │   │   │   ├── RegisterUserResponse.java     # 등록 응답 DTO
│   │   │   │   ├── AllowUserResponse.java        # 허용 응답 DTO
│   │   │   │   ├── AllowedUserResponse.java      # 허용 여부 응답 DTO
│   │   │   │   └── RankNumberResponse.java       # 순번 응답 DTO
│   │   │   ├── exception/
│   │   │   │   ├── ErrorCode.java                # 에러 코드 정의
│   │   │   │   ├── ApplicationException.java     # 커스텀 예외
│   │   │   │   └── ApplicationAdvice.java        # 전역 예외 처리
│   │   │   └── FlowApplication.java              # 메인 애플리케이션
│   │   └── resources/
│   │       ├── application.yaml                   # 애플리케이션 설정
│   │       └── templates/
│   │           └── waiting-room.html             # 대기실 페이지
│   └── test/
│       └── java/com/ticketing/flow/
│           └── service/
│               └── UserQueueServiceTest.java      # 서비스 테스트
└── build.gradle                                   # 빌드 설정
```

## 시스템 흐름도

### 1. 사용자 등록 및 대기열 진입

```
사용자 요청
    ↓
POST /api/v1/queue?queue=default&user_id={userId}
    ↓
UserQueueService.registerWaitQueue()
    ↓
Redis Sorted Set에 사용자 추가
(Key: users:queue:{queue}:wait, Score: Unix Timestamp)
    ↓
순번 반환 (Rank + 1)
```

### 2. 대기실 페이지 접근

```
GET /waiting-room?queue=default&user_id={userId}&redirect_url={url}
    ↓
쿠키에서 토큰 확인
    ↓
토큰 검증 (isAllowedUserByToken)
    ↓
[허용된 경우] → redirect_url로 리다이렉트
[대기 중인 경우] → waiting-room.html 렌더링
    ↓
클라이언트에서 3초마다 순번 조회 (GET /api/v1/queue/rank)
```

### 3. 자동 허용 처리 (스케줄러)

```
@Scheduled(initialDelay = 5000, fixedDelay = 3000)
    ↓
모든 대기열 스캔 (users:queue:*:wait)
    ↓
각 대기열에서 최대 100명 허용
    ↓
Wait Queue → Proceed Queue 이동
(users:queue:{queue}:wait → users:queue:{queue}:proceed)
```

### 4. 수동 허용 처리

```
POST /api/v1/queue/allow?queue=default&count={count}
    ↓
UserQueueService.allowUser()
    ↓
Wait Queue에서 최소값(count개) 추출
    ↓
Proceed Queue에 추가
    ↓
허용된 사용자 수 반환
```

### 5. 진입 가능 여부 확인

```
GET /api/v1/queue/allowed?queue=default&user_id={userId}&token={token}
    ↓
토큰 생성 (SHA-256 해시)
    ↓
제공된 토큰과 비교
    ↓
일치 여부 반환
```

### 전체 시스템 흐름

```
┌─────────────┐
│   사용자    │
└──────┬──────┘
       │
       │ 1. 대기열 등록
       ↓
┌─────────────────────┐
│ UserQueueController │
│  POST /api/v1/queue │
└──────┬──────────────┘
       │
       ↓
┌─────────────────────┐
│  UserQueueService   │
│  registerWaitQueue  │
└──────┬──────────────┘
       │
       ↓
┌─────────────────────┐
│   Redis (Wait Set)  │
│ users:queue:*:wait  │
└──────┬──────────────┘
       │
       │ 2. 순번 조회 (3초마다)
       ↓
┌─────────────────────┐
│  GET /api/v1/queue/ │
│       rank          │
└──────┬──────────────┘
       │
       │ 3. 스케줄러 자동 허용
       ↓
┌──────────────────────────┐
│ UserQueueScheduleService  │
│   (3초마다 실행)          │
└──────┬───────────────────┘
       │
       ↓
┌─────────────────────┐
│  UserQueueService   │
│    allowUser        │
└──────┬──────────────┘
       │
       ↓
┌─────────────────────┐
│ Redis (Proceed Set) │
│users:queue:*:proceed│
└──────┬──────────────┘
       │
       │ 4. 진입 가능 여부 확인
       ↓
┌─────────────────────┐
│ GET /api/v1/queue/  │
│      allowed        │
└──────┬──────────────┘
       │
       ↓
┌─────────────┐
│  서비스 접근│
└─────────────┘
```

## 주요 기능

### 1. 대기열 등록
- 사용자를 대기열에 등록하고 현재 순번을 반환합니다.
- 이미 등록된 사용자는 에러를 반환합니다.

### 2. 순번 조회
- 대기 중인 사용자의 현재 순번을 실시간으로 조회할 수 있습니다.
- 등록되지 않은 사용자는 -1을 반환합니다.

### 3. 사용자 허용
- 대기열에서 사용자를 추출하여 진입 허용 목록으로 이동시킵니다.
- 수동 허용과 스케줄러를 통한 자동 허용을 지원합니다.

### 4. 진입 가능 여부 확인
- 토큰 기반으로 사용자의 진입 가능 여부를 확인합니다.
- SHA-256 해시를 사용하여 안전한 토큰을 생성합니다.

### 5. 대기실 페이지
- 대기 중인 사용자에게 순번을 표시하는 웹 페이지를 제공합니다.
- 3초마다 자동으로 순번을 갱신합니다.

## API 엔드포인트

### REST API

| Method | Endpoint | 설명 | 파라미터 |
|--------|----------|------|----------|
| POST | `/api/v1/queue` | 대기열 등록 | `queue`, `user_id` |
| POST | `/api/v1/queue/allow` | 사용자 허용 | `queue`, `count` |
| GET | `/api/v1/queue/allowed` | 진입 가능 여부 | `queue`, `user_id`, `token` |
| GET | `/api/v1/queue/rank` | 순번 조회 | `queue`, `user_id` |
| GET | `/api/v1/queue/touch` | 토큰 갱신 | `queue`, `user_id` |

### 페이지

| Method | Endpoint | 설명 | 파라미터 |
|--------|----------|------|----------|
| GET | `/waiting-room` | 대기실 페이지 | `queue`, `user_id`, `redirect_url` |

## Redis 데이터 구조

### Wait Queue (대기열)
- **Key**: `users:queue:{queue}:wait`
- **Type**: Sorted Set
- **Score**: Unix Timestamp (등록 시간)
- **Member**: User ID

### Proceed Queue (허용된 사용자)
- **Key**: `users:queue:{queue}:proceed`
- **Type**: Sorted Set
- **Score**: Unix Timestamp (허용 시간)
- **Member**: User ID

## 설정

### application.yaml

```yaml
server:
  port: 9010

spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379

scheduler:
  enabled: true  # 스케줄러 활성화 여부
```

## 실행 방법

### 1. Redis 실행
```bash
redis-server
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 3. 테스트 실행
```bash
./gradlew test
```

## 테스트

프로젝트는 Embedded Redis를 사용하여 통합 테스트를 지원합니다.

주요 테스트 케이스:
- 대기열 등록
- 중복 등록 방지
- 사용자 허용
- 순번 조회
- 진입 가능 여부 확인
- 토큰 생성 및 검증

## 라이선스

이 프로젝트는 학습 목적으로 작성되었습니다.

