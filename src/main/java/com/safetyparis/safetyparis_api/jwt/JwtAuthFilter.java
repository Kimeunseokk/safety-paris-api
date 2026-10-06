package com.safetyparis.safetyparis_api.jwt;

import com.safetyparis.safetyparis_api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// @Component를 붙이지 않음 - 붙이면 Spring Boot가 일반 서블릿 필터로도 자동 등록해 두 번 실행됨.
// SecurityConfig에서 직접 생성해 Spring Security 필터 체인에만 넣는다.
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 토큰이 유효하면 SecurityContext에 "누가, 어떤 권한으로" 요청했는지 등록만 한다.
        // 거절(401/403)은 하지 않음 - 토큰이 없거나 무효여도 그냥 넘기고, 로그인/권한이 필요한
        // 주소인지는 SecurityConfig의 접근 규칙이 판단한다. (그래서 공개 경로 목록이 여기엔 없음)
        String token = resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmail(token);
            // 토큰은 유효해도 회원이 삭제됐을 수 있으므로 없으면 등록하지 않음(= 비로그인 취급)
            userRepository.findByEmail(email).ifPresent(user -> {
                // hasRole("ADMIN")은 "ROLE_ADMIN" 권한을 찾으므로 접두사를 붙여 등록
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

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
