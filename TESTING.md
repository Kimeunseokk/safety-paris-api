# 테스트 작성 노트

이 프로젝트에서 실제로 쓴 테스트 패턴(Mockito, AssertJ, JUnit5)을 정리한 참고 문서.

## 기본 전제 — 단위 테스트는 진짜 DB/Redis를 안 건드림

`@Mock`으로 만든 객체는 완전히 가짜(fake) 객체다. `given(...).willReturn(...)`으로 직접 지정한 상황만 재현할 뿐, 실제 MySQL/Redis에 뭐가 들어있는지와는 전혀 무관하게 동작한다. "진짜 DB까지 포함해서" 확인하고 싶으면 `@SpringBootTest` 기반의 통합 테스트를 별도로 만들어야 한다.

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

## 실행 명령어

```
./gradlew test                                                              # 전체 테스트
./gradlew test --tests "com.safetyparis.safetyparis_api.service.UserServiceTest"   # 클래스 지정
```

결과 리포트: `build/reports/tests/test/index.html`
