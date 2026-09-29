package com.safetyparis.safetyparis_api.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    // 로그인 없이 접근 가능한 경로 (회원가입/로그인/토큰 재발급).
    // startsWith가 아니라 정확히 일치하는지(contains)로 비교해야 함 - startsWith였다면
    // "/api/users/5"(보호 대상)도 "/api/users"로 시작한다는 이유로 통과돼버렸을 것.
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/users",
            "/api/users/login",
            "/api/users/recreate"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 이 필터는 등록된 모든 요청에 적용되므로, 나중에 Swagger/actuator 등을
        // 추가하면 그 경로들도 PUBLIC_PATHS에 넣어줘야 막히지 않음
        if (PUBLIC_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\": \"인증이 필요합니다.\"}");
            return;
        }

        // 검증만 하고 끝 - 누가 요청했는지(email)는 별도로 안 남김.
        // 지금은 컨트롤러가 URL의 id만 보고 조회해서 문제없지만, "내 정보 조회"처럼
        // "현재 로그인한 사용자가 누구인지" 알아야 하는 기능이 생기면 이 필터에서
        // jwtTokenProvider.getEmail(token) 결과를 request attribute 등으로 넘겨줘야 함
        // (Spring Security로 가면 이 역할을 SecurityContextHolder가 대신 해줌)
        filterChain.doFilter(request, response);
    }

    // "Bearer {token}" 형식의 Authorization 헤더에서 토큰 문자열만 추출
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
