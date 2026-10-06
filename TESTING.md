# 테스트 작성 노트

이 프로젝트에서 실제로 쓴 테스트 패턴(Mockito, AssertJ, JUnit5)을 정리한 참고 문서.

## 기본 전제 — 단위 테스트는 진짜 DB/Redis를 안 건드림

`@Mock`으로 만든 객체는 완전히 가짜(fake) 객체다. `given(...).willReturn(...)`으로 직접 지정한 상황만 재현할 뿐, 실제 MySQL/Redis에 뭐가 들어있는지와는 전혀 무관하게 동작한다. "진짜 DB까지 포함해서" 확인하고 싶으면 `@SpringBootTest` 기반의 통합 테스트를 별도로 만들어야 한다. (이 프로젝트에선 `SecurityConfigTest`가 그 방식 — 아래 "MockMvc로 접근 규칙 테스트" 참고)

## 클래스 준비

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
}
```

- `@ExtendWith(MockitoExtension.class)`: JUnit5에서 Mockito 어노테이션(`@Mock`, `@InjectMocks`)을 쓸 수 있게 활성화
- `@Mock`: 의존성을 진짜 대신 가짜로 대체
- `@InjectMocks`: 위의 `@Mock`들을 테스트 대상 클래스(`UserService`)의 생성자에 자동으로 주입

## 요청 DTO에 값 채우기 — `ReflectionTestUtils`

요청 DTO들은 `@NoArgsConstructor`만 있고 setter가 없어서, 테스트에서 값을 넣으려면 리플렉션으로 필드에 직접 접근해야 한다. 프로덕션 코드에 테스트 전용 setter를 넣지 않기 위한 선택.

```java
private UserLoginRequestDto loginRequest(String email, String password) {
    UserLoginRequestDto dto = new UserLoginRequestDto();
    ReflectionTestUtils.setField(dto, "email", email);
    ReflectionTestUtils.setField(dto, "password", password);
    return dto;
}
```

이렇게 헬퍼 메소드로 빼두면 여러 테스트에서 재사용 가능.

## 가짜 동작 지정 — `given().willReturn()` (BDDMockito)

```java
given(userRepository.existsByEmail("test@test.com")).willReturn(false);
given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
```

"이 메소드가 이 인자로 호출되면 이 값을 리턴해라"를 미리 정의. `Mockito.when(...).thenReturn(...)`과 완전히 같은 기능이지만, `given/willReturn`(BDD 스타일)이 문장으로 읽었을 때 더 자연스러워서 이쪽을 사용.

## 결과 검증 — AssertJ

```java
assertThat(response.getEmail()).isEqualTo("test@test.com");
```

- `assertThat(실제값).isEqualTo(기대값)` — 값 비교의 기본형
- 메소드 체이닝으로 여러 검증을 이어 쓸 수 있음 (`.isEqualTo(...).isNotNull()` 등)

## 예외 검증 — `assertThatThrownBy`

```java
assertThatThrownBy(() -> userService.signUpUser(requestDto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("이미 가입된 이메일입니다");
```

람다로 실행할 코드를 감싸서 넘기면, 그 안에서 예외가 던져지는지/어떤 타입인지/메시지에 뭐가 포함되는지까지 검증 가능.

## 호출 여부 검증 — `verify()`

```java
verify(userRepository, never()).save(any(User.class));
```

- `verify(mock).메소드(...)`: 그 메소드가 **호출됐는지** 확인
- `verify(mock, never()).메소드(...)`: **호출 안 됐는지** 확인 — 예외가 터진 뒤에 뒤쪽 로직(저장, 토큰 발급 등)이 실행되지 않았음을 보장하는 용도로 자주 씀
- `any(User.class)`: "User 타입이면 값 상관없이 매칭"하는 인자 매처. 정확한 객체 동일성까지 비교할 필요 없을 때 사용

## 테스트 하나의 구조 — Given / When / Then

```java
@Test
@DisplayName("회원가입 성공 - 중복 이메일이 없으면 암호화해서 저장하고 응답을 리턴한다")
void signUpUser_success() {
    // given - 가짜 상황 준비
    ...
    // when - 실제로 테스트 대상 메소드 실행
    UserResponseDto response = userService.signUpUser(requestDto);
    // then - 결과 검증
    assertThat(response.getEmail()).isEqualTo("test@test.com");
}
```

- `@DisplayName`: 테스트 결과 리포트에 표시될 한글 설명 — 메소드명만으로는 "무엇을 검증하는 테스트인지" 파악이 어려우니 꼭 작성
- 메소드명 컨벤션: `대상메소드_상황_결과` (예: `loginUser_wrongPassword_throwsException`)

## 실패 테스트를 쓰는 이유

성공 케이스만 테스트하면 "정상 흐름이 도는지"만 확인하는 셈. 실패 케이스(이메일 중복, 이메일 없음, 비밀번호 불일치 등)를 각각 테스트해야 "방어 로직이 의도한 대로 작동하는지"를 증명할 수 있다. 실제로 `recreateAccessToken()`에서 `!` 하나 빠뜨린 버그를 Postman으로 우연히 발견했던 적이 있는데, 이런 실패 테스트가 미리 있었다면 그 자리에서 바로 잡혔을 것.

## 테스트 목록

| 테스트 클래스 | 개수 | 종류 | 확인하는 것 |
|---|---|---|---|
| `UserServiceTest` | 9 | 단위 (Mockito) | 회원가입/로그인/토큰 재발급 성공·실패 |
| `JwtTokenProviderTest` | 7 | 단위 | 토큰 생성·검증·만료 |
| `JwtAuthFilterTest` | 6 | 단위 (Mockito) | 토큰이 유효할 때만 SecurityContext에 사용자 등록 |
| `MarkerServiceTest` | 1 | 단위 (Mockito) | 없는 마커 조회 시 `NoSuchElementException`(404) |
| `ReportServiceTest` | 3 | 단위 (Mockito) | 승인 시 Marker 저장, 거절 시 저장 안 함, 재승인 차단 |
| `SecurityConfigTest` | 7 | 통합 (MockMvc) | URL별 접근 규칙 — 공개 / 401 / 403 / 통과, 세미콜론 주소 우회 차단 |
| `SafetyparisApiApplicationTests` | 1 | 통합 | 애플리케이션 컨텍스트가 뜨는지 |

`SecurityConfigTest`와 `SafetyparisApiApplicationTests`는 스프링을 실제로 띄우기 때문에 **MySQL/Redis가 켜져 있어야 통과**한다 (DB 연결 실패 시 `Unable to determine Dialect` 에러).

## 서비스 로직의 "부수 효과" 검증 — 승인/거절 테스트

리턴값만 보면 승인과 거절은 똑같이 `AdminResponseDto`를 돌려준다. 둘의 진짜 차이는 **Marker를 저장하느냐**라서 그걸 `verify`로 확인한다.

```java
reportService.approveReport(1L);
verify(markerRepository).save(any(Marker.class));          // 승인: 저장됨

reportService.rejectReport(1L);
verify(markerRepository, never()).save(any(Marker.class)); // 거절: 저장 안 됨
```

실제로 거절 메소드에 승인 코드를 복사하다가 `markerRepository.save()`가 딸려 들어간 적이 있다 — 에러 없이 "거절한 제보가 지도에 뜨는" 버그라 눈으로는 놓치기 쉽다. 이 테스트가 있으면 바로 실패한다.

## 필터 테스트 — `SecurityContextHolder`

Spring Security 전환 후 `JwtAuthFilter`는 401/403을 직접 응답하지 않고, 토큰이 유효하면 **SecurityContext에 로그인 사용자를 등록**하는 일만 한다. 그래서 응답 코드가 아니라 "무엇이 등록됐는지"를 검증한다.

```java
jwtAuthFilter.doFilter(request, response, filterChain);

Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
assertThat(authentication.getPrincipal()).isEqualTo("user@test.com");
assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_USER");
```

- 토큰이 없거나 무효면 `getAuthentication()`이 `null`이어야 하고, 그래도 `filterChain.doFilter()`는 호출돼야 한다 (막을지는 SecurityConfig가 판단하므로)
- `extracting(...)`: 컬렉션의 각 원소에서 값을 뽑아 비교하는 AssertJ 기능. 권한 객체 목록을 문자열 목록으로 바꿔서 비교

```java
@AfterEach
void clearContext() {
    SecurityContextHolder.clearContext();
}
```

SecurityContext는 **스레드 단위 보관함**이라, 비우지 않으면 앞 테스트에서 등록한 사용자가 다음 테스트에 남아 결과가 꼬인다.

## MockMvc로 접근 규칙 테스트 — `SecurityConfigTest`

"USER는 403, ADMIN은 통과" 같은 규칙은 `SecurityConfig`에 선언돼 있어서, 클래스 하나를 단위 테스트하는 방식으로는 확인할 수 없다. 스프링을 띄우고 **실제 HTTP 요청을 Security 필터 체인에 통과**시켜 봐야 한다.

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;   // 진짜 서비스 대신 Mock → DB 데이터와 무관하게 "막히는지"만 확인
}
```

- `@SpringBootTest`: 애플리케이션 전체(SecurityConfig 포함)를 실제로 띄움
- `@AutoConfigureMockMvc`: 서버를 실제 포트로 띄우지 않고도 HTTP 요청을 흉내 내는 `MockMvc` 준비
- `@MockBean`: 스프링 안의 빈을 Mock으로 교체. `@Mock`과 달리 **스프링 컨테이너에 등록된 빈을 바꿔치기**한다

```java
mockMvc.perform(get("/api/admin/reports").with(loginAs("user@test.com", "USER")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("관리자 권한이 필요합니다."));
```

- `perform(...)`: 요청 보내기, `andExpect(...)`: 응답 검증
- `jsonPath("$.message")`: 응답 JSON의 `message` 필드 값 검증

### 로그인 상태 만들기 — `authentication()`

```java
private static RequestPostProcessor loginAs(String email, String role) {
    return authentication(new UsernamePasswordAuthenticationToken(
            email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
}
```

`spring-security-test`의 `authentication()`으로 요청에 로그인 사용자를 붙인다. 진짜 토큰을 발급받지 않아도 된다.

흔히 쓰는 `@WithMockUser`를 안 쓴 이유: `@WithMockUser`는 principal을 Spring의 `User` 객체로 넣는데, 이 프로젝트의 `JwtAuthFilter`는 principal을 **email 문자열**로 넣는다. 형태가 다르면 컨트롤러의 `@AuthenticationPrincipal String email`이 `null`이 돼서 실제 동작과 다른 테스트가 된다. 그래서 필터와 똑같은 형태로 직접 만들었다.

```java
verify(reportService).createReport(eq("reporter@test.com"), any());
```

컨트롤러가 principal의 email을 서비스에 제대로 넘기는지도 함께 확인.

### MockMvc로 확인 못 하는 것 — `/error`

검증 실패(`@Valid`) 같은 예외가 나면 실제 서버(톰캣)는 내부적으로 `/error` 주소로 다시 요청을 보내는데, MockMvc는 톰캣이 없어서 이 과정을 재현하지 않는다. `SecurityConfig`에서 `/error`를 막으면 400이 401로 바뀌는 문제가 있는데, 이건 MockMvc로는 안 잡혀서 **서버를 띄워 curl/Postman으로 확인**했다 (토큰 있는 상태에서 필수값 빠진 제보 → 400).

## 실행 명령어

```
./gradlew test                                                              # 전체 테스트
./gradlew test --tests "com.safetyparis.safetyparis_api.service.UserServiceTest"   # 클래스 지정
./gradlew test --tests "*SecurityConfigTest"                                      # 이름으로 지정 (MySQL/Redis 필요)
```

결과 리포트: `build/reports/tests/test/index.html`
