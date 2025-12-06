package com.ssafy.yumcoach.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    private final Key key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtUtil(
            @Value("${jwt.secret-base64}") String secretBase64,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) {
        byte[] secretBytes = Base64.getDecoder().decode(secretBase64);
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(int userId) {
        return createToken(userId, accessTokenValidity);
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(int userId) {
        return createToken(userId, refreshTokenValidity);
    }

    /**
     * 토큰 생성 (밀리초 단위)
     */
    private String createToken(int userId, long ttlMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .claim("userId", userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 파싱 및 검증
     */
    public Jws<Claims> parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(30)
                    .build()
                    .parseClaimsJws(token);
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.error("Invalid token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 토큰에서 userId 추출
     */
    public int getUserId(String token) {
        return parse(token).getBody().get("userId", Integer.class);
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
