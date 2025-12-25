package com.ssafy.yumcoach.auth.util;

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
     * 
     * @param userId 사용자 ID
     * @return JWT Access Token (유효기간: 1시간)
     *         주의: HttpOnly Cookie로 저장하여 XSS 공격 방지
     */
    public String createAccessToken(Integer userId) {
        return createToken(userId, null, accessTokenValidity);
    }

    /**
     * Access Token 생성 (주제(subject) 포함)
     * 
     * @param userId  사용자 ID (숫자, Kakao 등 외부 계정은 0 등 예약 값 가능)
     * @param subject 이메일 등 주제
     */
    public String createAccessToken(Integer userId, String subject) {
        return createToken(userId, subject, accessTokenValidity);
    }

    /**
     * Refresh Token 생성
     * 
     * @param userId 사용자 ID
     * @return JWT Refresh Token (유효기간: 7일)
     *         주의: DB에 저장 필수, Access Token 갱신용
     */
    public String createRefreshToken(Integer userId) {
        return createToken(userId, null, refreshTokenValidity);
    }

    /**
     * 토큰 생성 (밀리초 단위)
     */
    private String createToken(Integer userId, String subject, long ttlMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .claim("userId", userId)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(ttlMillis)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 파싱 및 검증
     * 
     * @param token JWT 토큰 문자열
     * @return 파싱된 Claims (userId 등 포함)
     * @throws ExpiredJwtException 토큰 만료 시
     *                             주의: 30초 시간 오차 허용 (서버 간 시간 차이 대응)
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
     * 
     * @param token JWT 토큰 문자열
     * @return 사용자 ID
     *         주의: 토큰 검증 후 사용자 식별에 활용
     */
    public Integer getUserId(String token) {
        return parse(token).getBody().get("userId", Integer.class);
    }

    /**
     * 토큰 유효성 검증
     * 
     * @param token JWT 토큰 문자열
     * @return true: 유효, false: 무효
     *         주의: 만료, 서명 오류 등 모든 예외를 false로 반환
     */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmail(String token) {
        return parse(token).getBody().getSubject();
    }
}
