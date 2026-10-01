package com.safetyparis.safetyparis_api.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("공개 경로(회원가입)는 토큰 검사 없이 통과시킨다")
    void publicPath_signup_passesThrough() throws Exception {
        given(request.getRequestURI()).willReturn("/api/users");

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("공개 경로(로그인, 재발급)도 토큰 검사 없이 통과시킨다")
    void publicPath_loginAndRecreate_passThrough() throws Exception {
        given(request.getRequestURI()).willReturn("/api/users/login");

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("공개 경로(마커 상세)는 id가 붙어도 토큰 검사 없이 통과시킨다")
    void publicPath_markerDetail_passesThrough() throws Exception {
        given(request.getRequestURI()).willReturn("/api/markers/5");

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("보호된 경로에 Authorization 헤더가 없으면 401을 응답하고 다음 단계로 넘기지 않는다")
    void protectedPath_noToken_returns401() throws Exception {
        given(request.getRequestURI()).willReturn("/api/users/5");
        given(request.getHeader("Authorization")).willReturn(null);
        given(response.getWriter()).willReturn(mock(PrintWriter.class));

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아니면 토큰을 못 꺼내 401을 응답한다")
    void protectedPath_malformedHeader_returns401() throws Exception {
        given(request.getRequestURI()).willReturn("/api/users/5");
        given(request.getHeader("Authorization")).willReturn("InvalidFormat some-token");
        given(response.getWriter()).willReturn(mock(PrintWriter.class));

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("보호된 경로에 유효하지 않은 토큰이면 401을 응답한다")
    void protectedPath_invalidToken_returns401() throws Exception {
        given(request.getRequestURI()).willReturn("/api/users/5");
        given(request.getHeader("Authorization")).willReturn("Bearer invalid-token");
        given(jwtTokenProvider.validateToken("invalid-token")).willReturn(false);
        given(response.getWriter()).willReturn(mock(PrintWriter.class));

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("보호된 경로에 유효한 토큰이면 다음 단계로 통과시킨다")
    void protectedPath_validToken_passesThrough() throws Exception {
        given(request.getRequestURI()).willReturn("/api/users/5");
        given(request.getHeader("Authorization")).willReturn("Bearer valid-token");
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
