package com.safetyparis.safetyparis_api.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        // application.yml의 secret은 base64 문자열이라, 서명에 쓰려면 디코딩해서 실제 키 바이트로 변환해야 함
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // 로그인 성공 시 발급, 만료 짧게(15분) - 매 요청마다 이 토큰으로 인증 확인
    public String createAccessToken(String email) {
        return createToken(email, accessTokenExpiration);
    }

    // Access Token 재발급용, 만료 길게(3일) - Redis에 저장해두고 검증할 때 사용
    public String createRefreshToken(String email) {
        return createToken(email, refreshTokenExpiration);
    }

    // Redis에 저장할 때 TTL로 쓰기 위해 만료시간(ms)을 그대로 노출
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    private String createToken(String email, long expiration) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email) // 토큰의 주인이 누구인지(email)를 기록
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // 토큰에서 이메일(subject) 꺼내기 - 재발급/Redis 조회 시 사용
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // 서명 위조, 만료 등으로 파싱이 실패하면 예외가 던져지므로 잡아서 true/false로 변환
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key) // 서명이 우리 key로 만들어진 게 맞는지 검증
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
