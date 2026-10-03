# Safety-Paris API

파리 여행 중 소매치기 등 경범죄 정보를 사용자들이 제보/공유하여 예방에 기여하는 웹 서비스의 백엔드 API입니다.

> 원본 기획: `소프트웨어공학_계획서.pdf` (SRS) 기반으로 재설계

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
- [ ] Redis 캐싱 (마커 목록 등)

## 남은 설계 고려사항

- 관리자 인증: `User`의 `role` 필드(`USER`/`ADMIN`) 방식으로 구현 (별도 Admin 엔티티 분리는 미채택)
- 관리자 인가는 `JwtAuthFilter`에서 `/api/admin/**` 요청마다 DB로 role 조회 — 관리자 요청이 많아지면 JWT에 role을 넣어 조회 생략 고려
- 마커 "임의 수정 방지"는 수정/삭제 API를 아예 제공하지 않는 방식으로 해결 (Marker는 관리자 승인으로만 생성)
- 장난성 제보 필터링은 관리자 거절로 처리, Rate Limiting은 시간 되면 적용
- 관리자 계정은 일반 회원가입 후 DB에서 role을 직접 변경해 지정 (`UPDATE users SET role = 'ADMIN' WHERE email = ...`) — 회원가입으로 관리자가 될 수 없게 하기 위함
