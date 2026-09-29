package com.safetyparis.safetyparis_api.service;

import com.safetyparis.safetyparis_api.dto.*;
import com.safetyparis.safetyparis_api.entity.RefreshToken;
import com.safetyparis.safetyparis_api.entity.User;
import com.safetyparis.safetyparis_api.jwt.JwtTokenProvider;
import com.safetyparis.safetyparis_api.repository.RefreshTokenRepository;
import com.safetyparis.safetyparis_api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private UserSignUpRequestDto signUpRequest(String email, String password, String nickname) {
        UserSignUpRequestDto dto = new UserSignUpRequestDto();
        ReflectionTestUtils.setField(dto, "email", email);
        ReflectionTestUtils.setField(dto, "password", password);
        ReflectionTestUtils.setField(dto, "nickname", nickname);
        return dto;
    }

    private RefreshTokenRequestDto refreshTokenRequest(String token) {
        RefreshTokenRequestDto dto = new RefreshTokenRequestDto();
        ReflectionTestUtils.setField(dto, "token", token);
        return dto;
    }


    private UserLoginRequestDto loginRequest(String email, String password) {
        UserLoginRequestDto dto = new UserLoginRequestDto();
        ReflectionTestUtils.setField(dto, "email", email);
        ReflectionTestUtils.setField(dto, "password", password);
        return dto;
    }

    @Test
    @DisplayName("회원가입 성공 - 중복 이메일이 없으면 암호화해서 저장하고 응답을 리턴한다")
    void signUpUser_success() {
        // given
        UserSignUpRequestDto requestDto = signUpRequest("test@test.com", "password123", "tester");
        User savedUser = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("tester")
                .build();

        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        UserResponseDto response = userService.signUpUser(requestDto);

        // then
        assertThat(response.getEmail()).isEqualTo("test@test.com");
        assertThat(response.getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 가입된 이메일이면 예외를 던지고 저장하지 않는다")
    void signUpUser_duplicateEmail_throwsException() {
        // given
        UserSignUpRequestDto requestDto = signUpRequest("test@test.com", "password123", "tester");
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signUpUser(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 가입된 이메일입니다");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공 - 이메일/비밀번호가 맞으면 토큰을 발급하고 Refresh Token을 Redis에 저장한다")
    void loginUser_success() {
        // given
        UserLoginRequestDto requestDto = loginRequest("test@test.com", "password123");
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("tester")
                .build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtTokenProvider.createAccessToken("test@test.com")).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken("test@test.com")).willReturn("refresh-token");
        given(jwtTokenProvider.getRefreshTokenExpiration()).willReturn(259200000L);

        // when
        LoginResponseDto response = userService.login(requestDto);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("test@test.com");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일이면 예외를 던진다")
    void loginUser_emailNotFound_throwsException() {
        // given
        UserLoginRequestDto requestDto = loginRequest("nobody@test.com", "password123");
        given(userRepository.findByEmail("nobody@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.login(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아이디 또는 비밀번호가 올바르지 않습니다");

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호가 틀리면 예외를 던진다")
    void loginUser_wrongPassword_throwsException() {
        // given
        UserLoginRequestDto requestDto = loginRequest("test@test.com", "wrongPassword");
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("tester")
                .build();

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.login(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아이디 또는 비밀번호가 올바르지 않습니다");

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("토큰 재발급 성공 - 유효한 refresh token이고 Redis 값과 일치하면 새 access token을 발급한다")
    void recreateAccessToken_success() {
        // given
        RefreshTokenRequestDto requestDto = refreshTokenRequest("refresh-token");
        RefreshToken savedToken = new RefreshToken("test@test.com", "refresh-token", 259200L);

        given(jwtTokenProvider.validateToken("refresh-token")).willReturn(true);
        given(jwtTokenProvider.getEmail("refresh-token")).willReturn("test@test.com");
        given(refreshTokenRepository.findById("test@test.com")).willReturn(Optional.of(savedToken));
        given(jwtTokenProvider.createAccessToken("test@test.com")).willReturn("new-access-token");

        // when
        TokenResponseDto response = userService.recreateAccessToken(requestDto);

        // then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 서명 위조/만료 등으로 유효하지 않은 토큰이면 예외를 던진다")
    void recreateAccessToken_invalidToken_throwsException() {
        // given
        RefreshTokenRequestDto requestDto = refreshTokenRequest("invalid-token");
        given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.recreateAccessToken(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않는 토큰입니다");

        verify(refreshTokenRepository, never()).findById(any());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰이 없으면(로그아웃 등) 예외를 던진다")
    void recreateAccessToken_noSavedToken_throwsException() {
        // given
        RefreshTokenRequestDto requestDto = refreshTokenRequest("refresh-token");
        given(jwtTokenProvider.validateToken("refresh-token")).willReturn(true);
        given(jwtTokenProvider.getEmail("refresh-token")).willReturn("test@test.com");
        given(refreshTokenRepository.findById("test@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.recreateAccessToken(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("재로그인해주세요");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰과 값이 다르면 예외를 던진다")
    void recreateAccessToken_tokenMismatch_throwsException() {
        // given
        RefreshTokenRequestDto requestDto = refreshTokenRequest("old-refresh-token");
        RefreshToken savedToken = new RefreshToken("test@test.com", "new-refresh-token", 259200L);

        given(jwtTokenProvider.validateToken("old-refresh-token")).willReturn(true);
        given(jwtTokenProvider.getEmail("old-refresh-token")).willReturn("test@test.com");
        given(refreshTokenRepository.findById("test@test.com")).willReturn(Optional.of(savedToken));

        // when & then
        assertThatThrownBy(() -> userService.recreateAccessToken(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("토큰이 유효하지 않습니다");

        verify(jwtTokenProvider, never()).createAccessToken(any());
    }
}
