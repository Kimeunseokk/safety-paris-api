package com.safetyparis.safetyparis_api.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    // 테스트 전용 시크릿 - 운영 키와 무관하며, HS512 서명에 필요한 길이(64바이트 이상)를 맞춘 임의의 base64 값
    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tcHJvdmlkZXItdW5pdC10ZXN0LWRvLW5vdC11c2UtaW4tcHJvZA==";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Spring 없이 직접 생성 - @Value는 Spring이 빈을 만들 때만 관여하므로 new로 만들면 그냥 평범한 파라미터
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, 900_000L, 259_200_000L);
    }

    @Test
    @DisplayName("Access Token 생성 - 토큰 안의 subject로 이메일을 그대로 꺼낼 수 있다")
    void createAccessToken_containsEmailAsSubject() {
        // when
        String token = jwtTokenProvider.createAccessToken("test@test.com");

        // then
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("Refresh Token 생성 - 토큰 안의 subject로 이메일을 그대로 꺼낼 수 있다")
    void createRefreshToken_containsEmailAsSubject() {
        // when
        String token = jwtTokenProvider.createRefreshToken("test@test.com");

        // then
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.getEmail(token)).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("validateToken - 정상 발급된 토큰은 유효하다고 판단한다")
    void validateToken_validToken_returnsTrue() {
        // given
        String token = jwtTokenProvider.createAccessToken("test@test.com");

        // when & then
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken - 서명이 다른 키로 만들어진 토큰(위조)은 무효로 판단한다")
    void validateToken_tokenSignedWithDifferentKey_returnsFalse() {
        // given - 다른 시크릿으로 만든 provider가 발급한 토큰
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "ZGlmZmVyZW50LXNlY3JldC1rZXktZm9yLXRlc3QtcHVycG9zZS1kby1ub3QtdXNlLWluLXByb2Q=",
                900_000L, 259_200_000L);
        String tokenFromOtherKey = otherProvider.createAccessToken("test@test.com");

        // when & then - 우리 provider의 key로는 검증 실패해야 함
        assertThat(jwtTokenProvider.validateToken(tokenFromOtherKey)).isFalse();
    }

    @Test
    @DisplayName("validateToken - 이미 만료된 토큰은 무효로 판단한다")
    void validateToken_expiredToken_returnsFalse() {
        // given - 만료시간을 음수로 줘서 발급 즉시 만료된 토큰을 만듦
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(TEST_SECRET, -1_000L, 259_200_000L);
        String expiredToken = expiredTokenProvider.createAccessToken("test@test.com");

        // when & then
        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("validateToken - 아예 토큰 형식이 아닌 문자열은 무효로 판단한다")
    void validateToken_malformedString_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("this-is-not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("getRefreshTokenExpiration - 생성자에 넣은 만료시간을 그대로 리턴한다")
    void getRefreshTokenExpiration_returnsConfiguredValue() {
        assertThat(jwtTokenProvider.getRefreshTokenExpiration()).isEqualTo(259_200_000L);
    }
}
