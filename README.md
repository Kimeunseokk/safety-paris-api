# Safety-Paris API

파리 여행 중 소매치기 등 경범죄 정보를 사용자들이 제보/공유하여 예방에 기여하는 웹 서비스의 백엔드 API입니다.

> 원본 기획: `소프트웨어공학_계획서.pdf` (SRS) 기반으로 재설계

## 기술 스택

- Java 21 / Spring Boot 3.3.0
- Spring Web, Spring Data JPA
- MySQL
- Redis (Spring Data Redis) — 캐싱용
- Spring Security Crypto (BCrypt 비밀번호 암호화)
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
2. HelpLocation (단순 CRUD)
3. Marker (지도 조회)
4. Report (제보 + 관리자 승인/거절 — Report→Marker 전환 로직 포함, 가장 복잡)
5. Redis 캐싱 (위 기능들이 어느 정도 동작한 뒤 성능 개선 단계로 추가)

## API 명세

| 기능 | Method | Endpoint | 인증 |
|---|---|---|---|
| 회원가입 | POST | `/api/users` | 불필요 |
| 로그인 | POST | `/api/users/login` | 불필요 |
| 회원 조회 | GET | `/api/users/{id}` | 불필요 (추후 인가 필요) |
| 마커 목록 | GET | `/api/markers` | 불필요 |
| 마커 상세 | GET | `/api/markers/{id}` | 불필요 |
| 제보 등록 | POST | `/api/reports` | 사용자(선택) |
| 대기 제보 목록 | GET | `/api/admin/reports` | 관리자 |
| 제보 승인 | PATCH | `/api/admin/reports/{id}/approve` | 관리자 |
| 제보 거절 | PATCH | `/api/admin/reports/{id}/reject` | 관리자 |
| 도움기관 목록 | GET | `/api/help-locations` | 불필요 |

### 회원가입 — `POST /api/users`
```json
{ "email": "string", "password": "string", "nickname": "string" }
```

### 로그인 — `POST /api/users/login`
```json
{ "email": "string", "password": "string" }
```

### 마커 상세 조회 응답 예시
```json
{
  "id": 1,
  "latitude": 48.8566,
  "longitude": 2.3522,
  "ageGroup": "20대",
  "gender": "남성",
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
- [x] 로그인 (`POST /api/users/login`) — 이메일/비밀번호 검증 (토큰 발급은 미구현)
- [x] 회원 조회 (`GET /api/users/{id}`)
- [ ] JWT 기반 인증/인가
- [ ] HelpLocation (도움기관 CRUD)
- [ ] Marker (지도 마커 조회)
- [ ] Report (제보 등록 + 관리자 승인/거절)
- [ ] Redis 캐싱 (마커 목록 등)

## 남은 설계 고려사항

- 관리자 인증: `Member`에 `role` 필드 추가 방식으로 진행 중 (별도 Admin 엔티티 분리는 미채택)
- `GET /api/users/{id}`는 현재 인가 체크가 없어 누구나 순차 id로 다른 회원 정보를 조회 가능 (IDOR) — 로그인 인증 붙을 때 함께 처리 필요
- 마커 상세 조회 시 "다른 사용자가 임의로 수정하지 못하게" 하는 인가 로직 필요
- 장난성 제보 필터링(관리자 거절 처리), Rate Limiting은 시간 되면 적용
- 관리자 계정 최초 생성 방식 결정 필요 (DB 직접 삽입 vs 별도 API)
