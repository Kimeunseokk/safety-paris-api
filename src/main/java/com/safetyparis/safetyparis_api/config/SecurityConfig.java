package com.safetyparis.safetyparis_api.config;

import com.safetyparis.safetyparis_api.jwt.JwtAuthFilter;
import com.safetyparis.safetyparis_api.jwt.JwtTokenProvider;
import com.safetyparis.safetyparis_api.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWT로 인증하는 REST API라 세션/쿠키 기반 기능은 전부 끔
                // (CSRF는 쿠키 인증을 노리는 공격이라 헤더로 토큰을 보내는 방식엔 해당 없음)
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL별 접근 규칙 - 위에서부터 순서대로 검사, 처음 맞는 규칙이 적용됨
                .authorizeHttpRequests(auth -> auth
                        // 예외 발생 시 Spring이 내부적으로 /error로 넘기는데, 막혀 있으면 400/404 대신 401이 나감
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login", "/api/users/recreate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/help-locations", "/api/markers", "/api/markers/*").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 기존 JwtAuthFilter와 같은 응답 형식 유지
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다."))
                        .accessDeniedHandler((request, response, e) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 필요합니다."))
                )

                // 토큰이 유효하면 SecurityContext에 로그인 사용자를 등록 - 이후 위 규칙들이 그 정보로 판단
                .addFilterBefore(new JwtAuthFilter(jwtTokenProvider, userRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }
}
