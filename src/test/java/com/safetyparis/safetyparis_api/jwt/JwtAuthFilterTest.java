package com.safetyparis.safetyparis_api.jwt;

import com.safetyparis.safetyparis_api.entity.Role;
import com.safetyparis.safetyparis_api.entity.User;
import com.safetyparis.safetyparis_api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 필터는 이제 거절(401/403)하지 않고 "토큰이 유효하면 SecurityContext에 사용자 등록"만 한다.
// 401/403 접근 규칙은 SecurityConfigTest에서 실제 요청으로 확인.
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    // SecurityContext는 스레드 단위 보관함이라 테스트끼리 섞이지 않게 매번 비움
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User userWithRole(Role role) {
        User user = User.builder().email("user@test.com").password("encoded").nickname("tester").build();
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }

    private void givenValidToken(String email) {
        given(request.getHeader("Authorization")).willReturn("Bearer valid-token");
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.getEmail("valid-token")).willReturn(email);
    }

    @Test
    @DisplayName("유효한 토큰이면 email을 principal로, role을 ROLE_ 권한으로 등록하고 다음 단계로 넘긴다")
    void validToken_registersAuthentication() throws Exception {
        givenValidToken("user@test.com");
        given(userRepository.findByEmail("user@test.com")).willReturn(Optional.of(userWithRole(Role.USER)));

        jwtAuthFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo("user@test.com");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("ADMIN 회원이면 ROLE_ADMIN 권한으로 등록한다 - hasRole(\"ADMIN\")이 이 값을 찾음")
    void validToken_admin_registersRoleAdmin() throws Exception {
        givenValidToken("admin@test.com");
        given(userRepository.findByEmail("admin@test.com")).willReturn(Optional.of(userWithRole(Role.ADMIN)));

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 아무것도 등록하지 않고 다음 단계로 넘긴다 (막을지는 SecurityConfig가 판단)")
    void noToken_passesWithoutAuthentication() throws Exception {
        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 토큰으로 보지 않고 등록하지 않는다")
    void notBearer_passesWithoutAuthentication() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Basic abc123");

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 등록하지 않는다")
    void invalidToken_passesWithoutAuthentication() throws Exception {
        given(request.getHeader("Authorization")).willReturn("Bearer bad-token");
        given(jwtTokenProvider.validateToken("bad-token")).willReturn(false);

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰은 유효해도 회원이 삭제됐으면 등록하지 않는다 (비로그인 취급)")
    void validToken_deletedUser_passesWithoutAuthentication() throws Exception {
        givenValidToken("gone@test.com");
        given(userRepository.findByEmail("gone@test.com")).willReturn(Optional.empty());

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
