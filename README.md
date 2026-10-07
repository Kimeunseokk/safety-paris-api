# Safety-Paris API

[![CI](https://github.com/Kimeunseokk/safety-paris-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Kimeunseokk/safety-paris-api/actions/workflows/ci.yml)

파리 여행 중 소매치기 등 경범죄 정보를 사용자들이 제보/공유하여 예방에 기여하는 웹 서비스의 백엔드 API입니다.

> 원본 기획: `소프트웨어공학_계획서.pdf` (SRS) 기반으로 재설계

## 배포

- **API 서버**: http://13.209.19.229:8080
- 바로 확인해 볼 수 있는 공개 API (로그인 불필요)
  - 도움기관 목록: http://13.209.19.229:8080/api/help-locations
  - 마커 목록: http://13.209.19.229:8080/api/markers
- 구성: AWS EC2 1대에 Docker Compose로 Spring Boot + MySQL + Redis 실행 (외부에는 8080만 공개)
- 배포 방식: 로컬에서 서버용(linux/amd64) 이미지를 빌드해 서버로 전송 → 서버는 빌드 없이 실행만 (1GB 서버라 빌드 부담 분리), DB 스키마는 Flyway로 자동 적용

## 기술 스택

- Java 21 / Spring Boot 3.3.0
- Spring Web, Spring Data JPA
- MySQL
- Redis (Spring Data Redis) — Refresh Token 저장 및 추후 캐싱용
- Spring Security Crypto (BCrypt 비밀번호 암호화)
- JWT (io.jsonwebtoken/jjwt) — Access/Refresh Token 발급·검증
- Lombok, Bean Validation
- spring-dotenv (`.env` 환경변수 로드)

## 도메인 개념

| 개념 | 설명 |
|---|---|
| Member | 일반 사용자 (제보 작성). `role`로 일반/관리자 구분 |
| Report | 사용자가 보낸 원본 제보. 관리자 승인 전 `PENDING` 상태 |
| Marker | 관리자가 승인해서 지도에 노출되는 범죄 지점 |
| HelpLocation | 대사관, 경찰서 등 도움받을 수 있는 기관의 위치/연락처 |

**핵심 흐름**: 사용자 제보 → `Report` 저장(`PENDING`) → 관리자 검토 → 승인 시 `Marker`로 전환

## 개발 순서

**계층**: Entity → Repository → Service → Controller (하위 계층부터 구현)

**도메인** (단순 → 복잡):
1. Member (회원가입/로그인)
2. HelpLocation (도움기관 목록 조회)
3. Marker (지도 조회)
4. Report (제보 + 관리자 승인/거절 — Report→Marker 전환 로직 포함, 가장 복잡)
5. Redis 캐싱 (위 기능들이 어느 정도 동작한 뒤 성능 개선 단계로 추가)

## API 명세

| 기능 | Method | Endpoint | 인증 |
|---|---|---|---|
| 회원가입 | POST | `/api/users` | 불필요 |
| 로그인 | POST | `/api/users/login` | 불필요 |
| 토큰 재발급 | POST | `/api/users/recreate` | 불필요 (Refresh Token 제출) |
| 회원 조회 | GET | `/api/users/{id}` | 필요 (Access Token) |
| 마커 목록 | GET | `/api/markers` | 불필요 |
| 마커 상세 | GET | `/api/markers/{id}` | 불필요 |
| 제보 등록 | POST | `/api/reports` | 필요 (로그인한 회원만) |
| 대기 제보 목록 | GET | `/api/admin/reports` | 관리자 |
| 제보 승인 | PATCH | `/api/admin/reports/{id}/approve` | 관리자 |
| 제보 거절 | PATCH | `/api/admin/reports/{id}/reject` | 관리자 |
| 도움기관 목록 | GET | `/api/help-locations` | 불필요 |

### 회원가입 — `POST /api/users`
```json
{ "email": "string", "password": "string", "nickname": "string" }
```

### 로그인 — `POST /api/users/login`
Request:
```json
{ "email": "string", "password": "string" }
```
Response:
```json
{
  "user": { "id": 1, "email": "string", "nickname": "string" },
  "accessToken": "eyJhbGciOiJIUzUxMiJ9... (15분 만료)",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9... (3일 만료, Redis에도 저장됨)"
}
```

### 토큰 재발급 — `POST /api/users/recreate`
Request:
```json
{ "token": "만료된 Access Token 대신 제출하는 Refresh Token" }
```
Response:
```json
{ "accessToken": "새로 발급된 토큰", "refreshToken": "기존 값 그대로" }
```
Refresh Token은 서명 검증뿐 아니라 Redis에 저장된 값과 완전히 일치해야만 재발급됨 (로그아웃/탈취 대응).

### 마커 상세 조회 응답 예시
```json
{
  "id": 1,
  "latitude": 48.8566,
  "longitude": 2.3522,
  "ageGroup": "20대",
  "gender": "남성",
  "clothing": "검은 후드티, 청바지",
  "race": "백인",
  "headCount": 2,
  "height": "180cm대",
  "build": "보통",
  "hasBeard": true,
  "hasGlasses": false,
  "locationDescription": "에펠탑 근처 지하철역 출구",
  "stolenItems": "지갑, 휴대폰",
  "storyContent": "역에서 나오는 순간 어깨를 부딪히며...",
  "createdAt": "2024-07-01T10:00:00"
}
```
제보 등록(`POST /api/reports`) 시 인상착의 관련 필드는 전부 선택 입력(nullable)입니다 — 사용자가 기억 못 할 수 있음을 고려.

### 도움기관 목록 응답 예시
```json
[
  {
    "id": 1,
    "name": "주프랑스 한국 대사관",
    "latitude": 48.8738,
    "longitude": 2.3040,
    "phoneNumber": "+33-1-4753-6996",
    "type": "EMBASSY"
  }
]
```

## 구현 현황

- [x] 회원가입 (`POST /api/users`) — 이메일 중복 체크, BCrypt 비밀번호 암호화
- [x] 로그인 (`POST /api/users/login`) — 이메일/비밀번호 검증 + Access/Refresh Token 발급
- [x] 토큰 재발급 (`POST /api/users/recreate`) — Refresh Token 검증 + Redis 대조
- [x] JWT 기반 인증/인가 — `JwtAuthFilter`로 보호 경로 토큰 검사(401) + `/api/admin/**`은 `ADMIN` role 검사(403), 토큰의 email을 컨트롤러로 전달
- [x] 회원 조회 (`GET /api/users/{id}`) — 인증 필요
- [x] `UserService`/`MarkerService`/`ReportService`/`JwtTokenProvider`/`JwtAuthFilter` 단위 테스트 (29개, `TESTING.md` 참고)
- [x] HelpLocation 도움기관 목록 조회 (`GET /api/help-locations`)
- [x] Marker 목록/상세 조회 (`GET /api/markers`, `GET /api/markers/{id}`) — 인증 불필요, 없는 id는 404
- [x] Report 제보 등록 (`POST /api/reports`) — 로그인한 회원만, 등록 시 `PENDING` 상태로 저장
- [x] Report 관리자 대기 목록/승인/거절 (`/api/admin/reports`) — `ADMIN`만, 승인 시 Marker로 전환되어 지도에 노출, 이미 처리된 제보는 재처리 불가
- [x] Redis 캐싱 — 마커 목록(`GET /api/markers`)을 `@Cacheable`로 캐싱, 제보 승인 시 `@CacheEvict`로 무효화

## 남은 설계 고려사항

- 관리자 인증: `User`의 `role` 필드(`USER`/`ADMIN`) 방식으로 구현 (별도 Admin 엔티티 분리는 미채택)
- 관리자 인가는 `JwtAuthFilter`에서 `/api/admin/**` 요청마다 DB로 role 조회 — 관리자 요청이 많아지면 JWT에 role을 넣어 조회 생략 고려
- 마커 "임의 수정 방지"는 수정/삭제 API를 아예 제공하지 않는 방식으로 해결 (Marker는 관리자 승인으로만 생성)
- 장난성 제보 필터링은 관리자 거절로 처리, Rate Limiting은 시간 되면 적용
- 관리자 계정은 일반 회원가입 후 DB에서 role을 직접 변경해 지정 (`UPDATE users SET role = 'ADMIN' WHERE email = ...`) — 회원가입으로 관리자가 될 수 없게 하기 위함

## Spring Security 전환

### 전환 동기

처음에는 JWT 인증이 내부적으로 어떻게 동작하는지 이해하기 위해, Spring Security 없이 `OncePerRequestFilter`를 상속한 `JwtAuthFilter` 하나로 인증과 인가를 직접 구현했습니다. 관리자 기능까지 완성한 뒤, 검증된 실무 표준인 Spring Security로 전환했습니다.

전환 전 `JwtAuthFilter`가 맡던 역할:

- 로그인 없이 접근 가능한 경로 목록(`PUBLIC_PATHS`) 관리
- 토큰 검증 실패 시 401 응답 직접 작성
- 요청 주소가 `/api/admin/`으로 시작하면 DB에서 role을 조회해 `ADMIN`이 아니면 403 응답 직접 작성
- 토큰의 email을 `request.setAttribute()`로 컨트롤러에 전달

직접 구현하면서 겪은 불편도 있었습니다. 새 API를 추가할 때마다 필터의 공개 경로 목록을 직접 고쳐야 했고, 실제로 마커 상세 조회(`/api/markers/{id}`)를 목록에 빠뜨려 로그인 없이 조회하면 401이 나는 버그가 있었습니다.

### 전환하며 발견한 문제 — 관리자 권한 우회

전환 과정에서 기존 필터를 다시 점검하다가, **일반 회원이 관리자 API를 호출할 수 있는 구멍**을 발견했습니다. 전환 직전 코드를 따로 띄워 일반 회원(USER) 토큰으로 확인한 결과:

| 요청 | 전환 전 (직접 구현 필터) | 전환 후 (Spring Security) |
|---|---|---|
| `GET /api/admin/reports` | 403 | 403 |
| `GET /api/admin;x=1/reports` | **200 — 우회됨** | 400 — 차단 |
| `PATCH /api/admin;x=1/reports/{id}/approve` | **200 — 일반 회원이 자기 제보를 승인, 지도에 마커 생성** | 400 — 차단 |

**원인**: 필터는 요청 주소를 문자열로 비교했습니다(`uri.startsWith("/api/admin/")`). `/api/admin;x=1/reports`는 이 조건에 걸리지 않아 관리자 검사를 건너뛰었는데, Spring MVC는 주소의 `;` 뒷부분(매트릭스 변수)을 무시하고 컨트롤러를 찾기 때문에 그대로 관리자 컨트롤러에 도달했습니다. **필터가 해석하는 주소와 Spring MVC가 해석하는 주소가 달라서** 생긴 문제입니다.

**전환 후 차단되는 이유**: Spring Security의 기본 `StrictHttpFirewall`이 `;`, 인코딩된 `/`, `..` 등 이런 식으로 악용될 수 있는 주소를 요청 단계에서 거절(400)합니다. 별도 설정 없이 기본으로 동작합니다.

→ 보안 로직을 직접 구현하면 주소 정규화 같은 세부 사항을 놓치기 쉽고, 이런 부분을 검증된 프레임워크에 맡기는 이유를 직접 확인한 경험이었습니다.

### 전환 후 구조

| 역할 | 전환 전 | 전환 후 |
|---|---|---|
| 토큰 검증 → 사용자 식별 | `JwtAuthFilter` | `JwtAuthFilter` (SecurityContext에 사용자·권한 등록만) |
| 공개 경로 / 로그인 필요 / 관리자 전용 규칙 | `JwtAuthFilter` 안의 `Set`과 `if`문 | `SecurityConfig`에 선언 (`permitAll`, `authenticated`, `hasRole("ADMIN")`) |
| 401 / 403 응답 | 필터에서 직접 작성 | `AuthenticationEntryPoint` / `AccessDeniedHandler` |
| 컨트롤러로 사용자 전달 | `request.setAttribute("email")` → `@RequestAttribute` | `SecurityContext` → `@AuthenticationPrincipal` |

### 전환 후 달라진 점

- 접근 규칙이 `SecurityConfig` 한 곳에 모여, 어떤 API가 누구에게 열려 있는지 한눈에 보이고 새 API는 규칙 한 줄만 추가하면 됨
- 공개 경로에 HTTP 메서드까지 지정해 더 엄격해짐 (예: 이전엔 `/api/users`가 모든 메서드에 열려 있었으나 이제 회원가입 POST만 공개)
- 관리자 검사가 `hasRole("ADMIN")` 한 줄로 대체됨
- 접근 규칙(공개 / 401 / 403 / 통과)을 MockMvc 통합 테스트(`SecurityConfigTest`)로 검증
- 정상 요청의 응답(상태 코드, 메시지)은 전환 전과 동일하게 유지해 클라이언트 영향 없음

### 전환하면서 주의한 점

- **필터 중복 실행 방지**: `@Component`가 붙은 필터는 Spring Boot가 일반 서블릿 필터로도 자동 등록해 두 번 실행되므로, `SecurityConfig`에서 직접 생성해 Security 필터 체인에만 등록
- **`/error` 경로 허용**: 예외 발생 시 톰캣이 내부적으로 `/error`로 요청을 넘기는데, 이 경로가 막혀 있으면 400 응답이 401로 바뀌므로 `permitAll` 처리
- **권한 접두사**: `hasRole("ADMIN")`은 `ROLE_ADMIN` 권한을 찾으므로 필터에서 `"ROLE_" + role`로 등록
