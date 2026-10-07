# Safety-Paris API

[![CI](https://github.com/Kimeunseokk/safety-paris-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Kimeunseokk/safety-paris-api/actions/workflows/ci.yml)

파리 여행 중 소매치기 등 경범죄 정보를 사용자들이 제보·공유하여 예방에 기여하는 웹 서비스의 백엔드 API입니다.

> 원본 기획: `소프트웨어공학_계획서.pdf` (SRS) 기반으로 재설계

## 배포

- **API 기본 주소**: `http://13.209.19.229:8080` (루트 `/`는 API가 없어 401 응답 — 아래 공개 API로 확인)
- **API 문서 (Swagger UI)**: http://13.209.19.229:8080/swagger-ui/index.html — 브라우저에서 전체 API 명세를 보고 직접 호출 가능
- 바로 확인해 볼 수 있는 공개 API (로그인 불필요)
  - 마커 목록: http://13.209.19.229:8080/api/markers
  - 마커 상세: http://13.209.19.229:8080/api/markers/1
  - 도움기관 목록: http://13.209.19.229:8080/api/help-locations
- 운영 DB의 마커는 실제 흐름(회원가입 → 제보 → 관리자 승인)으로 만든 **예시 데이터**입니다 (`[예시]` 표시)

## 주요 기능

**핵심 흐름**: 사용자 제보 → `Report` 저장(`PENDING`) → 관리자 검토 → 승인 시 `Marker`로 전환되어 지도에 노출

| 도메인 | 설명 | 기능 |
|---|---|---|
| User | 회원. `role`(`USER`/`ADMIN`)로 일반/관리자 구분 | 회원가입(BCrypt), 로그인(JWT Access/Refresh), 토큰 재발급(Redis 대조), 회원 조회 |
| Report | 사용자가 보낸 원본 제보 (승인 전 `PENDING`) | 제보 등록(로그인 회원만), 관리자 대기 목록·승인·거절 |
| Marker | 관리자가 승인해 지도에 노출되는 범죄 지점 | 목록·상세 조회(로그인 불필요), 목록은 Redis 캐싱 |
| HelpLocation | 대사관·경찰서 등 도움받을 수 있는 기관 | 목록 조회(로그인 불필요) |

- 인상착의 필드는 전부 선택 입력 — 피해자가 기억하지 못할 수 있음을 고려
- 마커 수정·삭제 API를 제공하지 않아 "임의 수정 방지" 요구사항 충족 (Marker는 관리자 승인으로만 생성)

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language / Framework | Java 21, Spring Boot 3.3.0, Spring Web, Spring Data JPA |
| 보안 | Spring Security (JWT 인증 필터, URL별 접근 제어), JWT (jjwt), BCrypt |
| DB / Cache | MySQL, Redis (Refresh Token 저장, 마커 목록 캐싱), Flyway (스키마·기준 데이터 버전 관리) |
| 테스트 | JUnit 5, Mockito, AssertJ, MockMvc, spring-security-test |
| 인프라 / 배포 | Docker, Docker Compose, AWS EC2, GitHub Actions (CI/CD), GHCR |
| API 문서 | springdoc-openapi (Swagger UI) |
| 기타 | Lombok, Bean Validation, spring-dotenv |

## 아키텍처 및 배포

### 구성

- AWS EC2 1대(1GB)에 Docker Compose로 Spring Boot + MySQL + Redis 실행
- 외부에는 8080만 공개, MySQL·Redis는 컨테이너 내부 네트워크에서만 접근
- 개발/운영 설정 분리: `application-dev.yml`(`ddl-auto: update`) / `application-prod.yml`(`ddl-auto: validate`, DB 주소·비밀번호·JWT 키는 환경변수로 주입)
- DB 스키마와 기준 데이터(도움기관)는 Flyway(`db/migration`)로 관리 → 빈 DB에도 앱 시작 시 자동 적용

### CI/CD (GitHub Actions)

```
main에 push
 ├─ test   : MySQL·Redis 컨테이너를 띄워 전체 테스트 실행 → 실패하면 여기서 중단(배포 안 됨)
 └─ deploy : linux/amd64 이미지 빌드 → GHCR에 latest + 커밋 해시 태그로 업로드
             → SSH로 EC2 접속 → 새 이미지 pull → 앱 컨테이너만 교체 → 헬스 체크
```

- 서버의 GHCR 로그인은 배포 중에만 유지하고 끝나면 로그아웃 (서버에 자격증명을 남기지 않음)
- 이미지를 커밋 해시로도 태깅해, 문제가 생기면 `IMAGE_TAG=<커밋 해시> docker compose up -d`로 이전 버전 복구

### 로컬 실행

```bash
# 운영과 같은 구성(앱 + MySQL + Redis)으로 실행 — .env의 DB_PASSWORD, JWT_SECRET_KEY 사용
docker compose up -d --build

# 테스트 (MySQL·Redis 필요)
./gradlew test
```

## API 명세

전체 명세와 요청/응답 형식은 **Swagger UI**에서 확인하고 바로 호출해 볼 수 있습니다.

- 배포 서버: http://13.209.19.229:8080/swagger-ui/index.html
- 로컬: http://localhost:8080/swagger-ui/index.html
- springdoc이 컨트롤러·DTO(검증 어노테이션 포함)를 읽어 OpenAPI 명세(`/v3/api-docs`)를 자동 생성 → 코드와 문서가 항상 일치

| 기능 | Method | Endpoint | 인증 |
|---|---|---|---|
| 회원가입 | POST | `/api/users` | 불필요 |
| 로그인 | POST | `/api/users/login` | 불필요 |
| 토큰 재발급 | POST | `/api/users/recreate` | 불필요 (Refresh Token 제출) |
| 회원 조회 | GET | `/api/users/{id}` | 로그인 |
| 마커 목록 | GET | `/api/markers` | 불필요 |
| 마커 상세 | GET | `/api/markers/{id}` | 불필요 |
| 도움기관 목록 | GET | `/api/help-locations` | 불필요 |
| 제보 등록 | POST | `/api/reports` | 로그인 |
| 대기 제보 목록 | GET | `/api/admin/reports` | 관리자 |
| 제보 승인 | PATCH | `/api/admin/reports/{id}/approve` | 관리자 |
| 제보 거절 | PATCH | `/api/admin/reports/{id}/reject` | 관리자 |

- 인증 실패 401 `{"message": "인증이 필요합니다."}`, 권한 부족 403 `{"message": "관리자 권한이 필요합니다."}`
- 로그인이 필요한 요청은 `Authorization: Bearer <accessToken>` 헤더 사용

<details>
<summary>요청/응답 예시</summary>

**회원가입** — `POST /api/users`
```json
{ "email": "user@example.com", "password": "password123", "nickname": "닉네임" }
```

**로그인** — `POST /api/users/login`
```json
// Request
{ "email": "user@example.com", "password": "password123" }
// Response
{
  "user": { "id": 1, "email": "user@example.com", "nickname": "닉네임" },
  "accessToken": "eyJhbGciOiJIUzUxMiJ9... (15분 만료)",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9... (3일 만료, Redis에도 저장)"
}
```

**토큰 재발급** — `POST /api/users/recreate`
```json
// Request
{ "token": "Refresh Token" }
// Response
{ "accessToken": "새로 발급된 토큰", "refreshToken": "기존 값 그대로" }
```
Refresh Token은 서명 검증뿐 아니라 Redis에 저장된 값과 완전히 일치해야 재발급됩니다 (탈취 대응).

**제보 등록** — `POST /api/reports` (인상착의 필드는 전부 선택)
```json
// Request
{
  "latitude": 48.8584, "longitude": 2.2945,
  "ageGroup": "20대", "gender": "남성", "clothing": "검은 후드티, 청바지", "race": "백인",
  "headCount": 2, "height": "180cm대", "build": "마른 체형", "hasBeard": true, "hasGlasses": false,
  "locationDescription": "에펠탑 앞 광장", "stolenItems": "지갑",
  "storyContent": "사진을 찍는 사이 지갑을 빼감"
}
// Response 201
{ "reportId": 1, "status": "PENDING", "createdAt": "2026-10-07T16:32:57" }
```

**마커 상세** — `GET /api/markers/{id}`
```json
{
  "id": 1, "latitude": 48.8584, "longitude": 2.2945,
  "ageGroup": "20대", "gender": "남성", "clothing": "검은 후드티, 청바지", "race": "백인",
  "headCount": 2, "height": "180cm대", "build": "마른 체형", "hasBeard": true, "hasGlasses": false,
  "locationDescription": "에펠탑 앞 광장", "stolenItems": "지갑",
  "storyContent": "사진을 찍는 사이 지갑을 빼감", "createdAt": "2026-10-07T16:32:57"
}
```

**도움기관 목록** — `GET /api/help-locations`
```json
[{ "id": 1, "name": "주프랑스 한국 대사관", "latitude": 48.8738, "longitude": 2.304,
   "phoneNumber": "+33-1-4753-6996", "type": "EMBASSY" }]
```
</details>

## 설계 결정

- **Report와 Marker 테이블 분리**: 지도 조회는 승인된 데이터만 담긴 `markers`만 읽으므로, 승인 전 제보가 실수로 노출될 여지가 구조적으로 없음
- **승인은 하나의 트랜잭션**: 상태 변경(`APPROVED`)과 Marker 생성을 한 트랜잭션으로 묶어 어긋난 데이터가 생기지 않게 함
- **중복 승인 방지**: `Report.approve()`/`reject()`가 `PENDING`일 때만 상태를 바꾸도록 엔티티에서 검사 → 같은 제보를 두 번 승인해도 마커가 중복 생성되지 않음
- **로그인 필수 제보**: 익명 제보 대신 작성자를 남겨 장난성 제보 추적 가능 (장난성 제보는 관리자 거절로 처리)
- **마커 목록 캐싱**: 모든 사용자가 지도를 열 때마다 조회하지만 관리자 승인 때만 바뀌는 데이터라 Redis에 캐싱(`@Cacheable`), 승인 시 `@CacheEvict`로 무효화
- **관리자 계정**: 일반 회원가입 후 DB에서 role을 직접 변경해 지정 — 회원가입으로 관리자가 될 수 없게 함
- **role은 DB에서 조회**: JWT에 role을 넣지 않고 인증된 요청마다 DB에서 조회 → 권한 변경이 즉시 반영됨 (트래픽이 많아지면 JWT에 포함 고려)
- **운영은 `ddl-auto: validate` + Flyway**: 앱이 운영 테이블을 임의로 바꾸지 않고, 스키마 변경 이력을 SQL 파일로 관리

## 트러블슈팅

### 1. 직접 구현한 JWT 필터의 관리자 권한 우회 → Spring Security 전환

처음에는 JWT 인증 원리를 이해하기 위해 Spring Security 없이 `OncePerRequestFilter`를 상속한 `JwtAuthFilter` 하나로 인증과 인가를 직접 구현했습니다. 공개 경로 목록(`PUBLIC_PATHS`), 401/403 응답 작성, `/api/admin/**`의 role 검사까지 모두 이 필터가 맡았고, 새 API마다 공개 목록을 직접 고쳐야 해서 마커 상세 조회를 빠뜨려 401이 나는 버그도 있었습니다. 관리자 기능까지 완성한 뒤 실무 표준인 Spring Security로 전환했습니다.

**전환하며 발견한 문제**: 기존 필터를 다시 점검하다가 **일반 회원이 관리자 API를 호출할 수 있는 구멍**을 발견했습니다. 전환 직전 코드를 따로 띄워 일반 회원(USER) 토큰으로 확인한 결과:

| 요청 | 전환 전 (직접 구현 필터) | 전환 후 (Spring Security) |
|---|---|---|
| `GET /api/admin/reports` | 403 | 403 |
| `GET /api/admin;x=1/reports` | **200 — 우회됨** | 400 — 차단 |
| `PATCH /api/admin;x=1/reports/{id}/approve` | **200 — 일반 회원이 자기 제보를 승인, 지도에 마커 생성** | 400 — 차단 |

- **원인**: 필터는 주소를 문자열로 비교(`uri.startsWith("/api/admin/")`)했는데, Spring MVC는 `;` 뒷부분(매트릭스 변수)을 무시하고 컨트롤러를 찾음 → **필터와 Spring MVC가 해석하는 주소가 달라서** 관리자 검사를 건너뜀
- **해결**: Spring Security의 기본 `StrictHttpFirewall`이 `;`, 인코딩된 `/`, `..` 같은 주소를 요청 단계에서 거절(400). 이 경우를 회귀 테스트(`SecurityConfigTest`)로 고정
- **배운 점**: 보안 로직을 직접 구현하면 주소 정규화 같은 세부 사항을 놓치기 쉽고, 이런 부분을 검증된 프레임워크에 맡기는 이유를 직접 확인

**전환 후 구조**

| 역할 | 전환 전 | 전환 후 |
|---|---|---|
| 토큰 검증 → 사용자 식별 | `JwtAuthFilter` | `JwtAuthFilter` (SecurityContext에 사용자·권한 등록만) |
| 공개 / 로그인 필요 / 관리자 전용 규칙 | 필터 안의 `Set`과 `if`문 | `SecurityConfig`에 선언 (`permitAll`, `authenticated`, `hasRole("ADMIN")`) |
| 401 / 403 응답 | 필터에서 직접 작성 | `AuthenticationEntryPoint` / `AccessDeniedHandler` |
| 컨트롤러로 사용자 전달 | `request.setAttribute("email")` | `@AuthenticationPrincipal` |

- 공개 경로에 HTTP 메서드까지 지정해 더 엄격해짐 (이전엔 `/api/users`가 모든 메서드에 열려 있었으나 이제 회원가입 POST만 공개)
- 정상 요청의 응답(상태 코드, 메시지)은 전환 전과 동일하게 유지해 클라이언트 영향 없음
- 주의한 점: `@Component` 필터의 서블릿 필터 중복 등록 방지, `/error` 경로 허용(막으면 400이 401로 바뀜), `hasRole`의 `ROLE_` 접두사

### 2. 1GB 서버의 메모리·디스크 한계 → 빌드와 실행 분리

EC2(메모리 1GB, 디스크 8GB)는 앱·MySQL·Redis를 띄우는 것만으로도 빠듯해, 서버에서 Gradle로 이미지까지 빌드하기엔 메모리와 디스크가 부족했습니다. 서버를 키우는 대신 **빌드는 GitHub Actions에서 하고 서버는 GHCR에서 이미지를 받아 실행만** 하도록 분리했고, 이 구조가 그대로 자동 배포(CD)로 이어졌습니다. 개발 Mac(ARM)과 서버(x86_64)의 CPU 아키텍처가 달라 이미지는 `linux/amd64`로 빌드합니다.

### 3. 컨테이너에서 `createdAt`이 실제보다 9시간 이른 시각으로 저장

로컬에서 운영과 같은 구성을 Docker Compose로 띄워 검증하던 중, 12시에 등록한 제보의 시각이 03시로 저장되는 것을 발견했습니다. 컨테이너의 기본 시간대가 UTC라 `LocalDateTime.now()`가 UTC 기준이었기 때문으로, 앱 컨테이너에 `TZ=Asia/Seoul`을 지정해 해결했습니다. 배포 전에 운영과 같은 환경을 먼저 띄워 본 덕분에 운영 데이터가 틀어지기 전에 잡았습니다.

### 4. 브라우저에서 JSON 한글이 깨져 보임

응답 데이터는 정상 UTF-8이었지만 `Content-Type: application/json`에 charset이 없어 일부 브라우저가 다른 인코딩으로 해석했습니다. `server.servlet.encoding.force: true`로 `charset=UTF-8`을 명시해 해결했습니다.

## 테스트

단위 테스트 + MockMvc 통합 테스트 **34개**, push마다 GitHub Actions에서 자동 실행됩니다. 자세한 작성 방식은 [`TESTING.md`](TESTING.md) 참고.

| 테스트 | 확인하는 것 |
|---|---|
| `UserServiceTest` | 회원가입·로그인·토큰 재발급 성공/실패 |
| `JwtTokenProviderTest` | 토큰 생성·검증·만료 |
| `JwtAuthFilterTest` | 유효한 토큰일 때만 SecurityContext에 사용자·권한 등록 |
| `ReportServiceTest` | 승인 시 Marker 저장, 거절 시 저장 안 함, 재승인 차단 |
| `MarkerServiceTest` | 없는 마커 조회 시 404 |
| `SecurityConfigTest` | URL별 접근 규칙(공개/401/403/통과), 세미콜론 주소 우회 차단 |

## 향후 개선

- Swagger UI에 JWT 인증(Authorize) 설정 — 로그인이 필요한 API도 브라우저에서 바로 호출 가능하게
- 도메인 연결 + HTTPS (현재는 IP·HTTP라 토큰이 평문 전송됨)
- 예외별 상태 코드 세분화 (로그인 실패 401, 중복 이메일·이미 처리된 제보 409)
- 제보 남용 방지를 위한 Rate Limiting
