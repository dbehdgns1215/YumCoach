package com.ssafy.yumcoach.controller;

import com.ssafy.yumcoach.controller.dto.*;
import com.ssafy.yumcoach.domain.RefreshToken;
import com.ssafy.yumcoach.domain.User;
import com.ssafy.yumcoach.domain.UserHealth;
import com.ssafy.yumcoach.service.RefreshTokenService;
import com.ssafy.yumcoach.service.UserService;
import com.ssafy.yumcoach.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    
    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            User user = User.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .name(request.getName())
                    .build();
            
            userService.signup(user);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "회원가입이 완료되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Signup error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "회원가입 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 로그인
     * Access Token: HttpOnly Cookie (JavaScript 접근 불가 - XSS 방어)
     * Refresh Token: HttpOnly Cookie (JavaScript 접근 불가 - XSS 방어)
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest request, HttpServletResponse response) {
        try {
            User user = userService.signin(request.getEmail(), request.getPassword());
            
            // JWT 토큰 생성
            String accessToken = jwtUtil.createAccessToken(user.getId());
            String refreshToken = jwtUtil.createRefreshToken(user.getId());
            
            // Refresh Token DB 저장
            RefreshToken refreshTokenEntity = RefreshToken.builder()
                    .userId(user.getId())
                    .token(refreshToken)
                    .expiresAt(LocalDateTime.now().plus(7, ChronoUnit.DAYS))
                    .build();
            refreshTokenService.saveRefreshToken(refreshTokenEntity);
            
            // Access Token을 HttpOnly Cookie로 설정 (XSS 방어)
            Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
            accessTokenCookie.setHttpOnly(true);  // JavaScript 접근 차단
            accessTokenCookie.setSecure(false);   // HTTPS only (개발: false, 운영: true)
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(60 * 60); // 1시간
            response.addCookie(accessTokenCookie);
            
            // Refresh Token을 HttpOnly Cookie로 설정 (XSS 방어)
            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
            refreshTokenCookie.setHttpOnly(true);  // JavaScript 접근 차단
            refreshTokenCookie.setSecure(false);   // HTTPS only (개발: false, 운영: true)
            refreshTokenCookie.setPath("/api/user/refresh"); // refresh API에서만 전송
            refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
            response.addCookie(refreshTokenCookie);
            
            // 응답 (사용자 정보만 반환, 토큰은 쿠키에)
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", user.getId());
            responseData.put("email", user.getEmail());
            responseData.put("name", user.getName());
            responseData.put("message", "로그인 성공");
            
            return ResponseEntity.ok(responseData);
            
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            log.error("Signin error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 로그아웃
     * Cookie 삭제 + DB에서 Refresh Token 삭제
     */
    @PostMapping("/signout")
    public ResponseEntity<?> signout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Cookie에서 Access Token 가져오기
            String token = getTokenFromCookie(request, "accessToken");
            if (token != null) {
                int userId = jwtUtil.getUserId(token);
                // DB에서 Refresh Token 삭제
                refreshTokenService.deleteByUserId(userId);
            }
            
            // Access Token Cookie 삭제
            Cookie accessTokenCookie = new Cookie("accessToken", null);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(0);
            response.addCookie(accessTokenCookie);
            
            // Refresh Token Cookie 삭제
            Cookie refreshTokenCookie = new Cookie("refreshToken", null);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/api/user/refresh");
            refreshTokenCookie.setMaxAge(0);
            response.addCookie(refreshTokenCookie);
            
            Map<String, String> responseData = new HashMap<>();
            responseData.put("message", "로그아웃 되었습니다.");
            return ResponseEntity.ok(responseData);
            
        } catch (Exception e) {
            log.error("Signout error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그아웃 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 토큰 갱신
     * Cookie에서 Refresh Token을 읽어 새 Access Token 발급
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Cookie에서 Refresh Token 가져오기
            String refreshToken = getTokenFromCookie(request, "refreshToken");
            if (refreshToken == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Refresh Token이 없습니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // Refresh Token 검증
            if (!jwtUtil.validateToken(refreshToken)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "유효하지 않은 토큰입니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // DB에서 토큰 확인
            RefreshToken tokenEntity = refreshTokenService.findByToken(refreshToken);
            if (tokenEntity == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "유효하지 않은 토큰입니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // 새 Access Token 발급 및 Cookie 설정
            String newAccessToken = jwtUtil.createAccessToken(tokenEntity.getUserId());
            Cookie accessTokenCookie = new Cookie("accessToken", newAccessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setSecure(false);
            accessTokenCookie.setPath("/");
            accessTokenCookie.setMaxAge(60 * 60); // 1시간
            response.addCookie(accessTokenCookie);
            
            Map<String, String> responseData = new HashMap<>();
            responseData.put("message", "토큰 갱신 성공");
            return ResponseEntity.ok(responseData);
            
        } catch (Exception e) {
            log.error("Token refresh error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "토큰 갱신 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 내 정보 조회
     * Cookie에서 Access Token을 읽어 사용자 인증
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(HttpServletRequest request) {
        try {
            // Cookie에서 Access Token 가져오기
            String token = getTokenFromCookie(request, "accessToken");
            if (token == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            int userId = jwtUtil.getUserId(token);
            
            User user = userService.findById(userId);
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "사용자를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            log.error("Get user info error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "사용자 정보 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 건강정보 조회
     * Cookie에서 Access Token을 읽어 사용자 인증
     */
    @GetMapping("/health")
    public ResponseEntity<?> getUserHealth(HttpServletRequest request) {
        try {
            // Cookie에서 Access Token 가져오기
            String token = getTokenFromCookie(request, "accessToken");
            if (token == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            int userId = jwtUtil.getUserId(token);
            
            UserHealth userHealth = userService.findUserHealthByUserId(userId);
            if (userHealth == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "건강정보를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            
            return ResponseEntity.ok(userHealth);
            
        } catch (Exception e) {
            log.error("Get user health error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "건강정보 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 건강정보 수정
     * Cookie에서 Access Token을 읽어 사용자 인증
     */
    @PutMapping("/health")
    public ResponseEntity<?> updateUserHealth(
            HttpServletRequest request,
            @RequestBody UpdateHealthRequest healthRequest) {
        try {
            // Cookie에서 Access Token 가져오기
            String token = getTokenFromCookie(request, "accessToken");
            if (token == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            int userId = jwtUtil.getUserId(token);
            
            UserHealth userHealth = UserHealth.builder()
                    .userId(userId)
                    .height(healthRequest.getHeight())
                    .weight(healthRequest.getWeight())
                    .diabetes(healthRequest.getDiabetes())
                    .highBloodPressure(healthRequest.getHighBloodPressure())
                    .hyperlipidemia(healthRequest.getHyperlipidemia())
                    .kidneyDisease(healthRequest.getKidneyDisease())
                    .build();
            
            userService.updateUserHealth(userHealth);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "건강정보가 수정되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Update user health error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "건강정보 수정 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Cookie에서 토큰 추출 헬퍼 메소드
     */
    private String getTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
