package com.ssafy.yumcoach.user.controller;

import com.ssafy.yumcoach.user.model.SigninRequest;
import com.ssafy.yumcoach.user.model.SignupRequest;
import com.ssafy.yumcoach.user.model.UpdateHealthRequest;
import com.ssafy.yumcoach.user.model.User;
import com.ssafy.yumcoach.user.model.UserHealth;
import com.ssafy.yumcoach.user.model.service.UserService;
import com.ssafy.yumcoach.auth.model.RefreshTokenDto;
import com.ssafy.yumcoach.auth.model.service.RefreshTokenService;
import com.ssafy.yumcoach.auth.util.JwtUtil;
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
     * @param request 회원가입 요청 (email, password, name)
     * @return 200 OK: 회원가입 성공, 400 Bad Request: 중복 이메일 등
     * 주의: 비밀번호는 평문 저장 (TODO: 암호화 필요)
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        System.out.println("Signup request: " + request);
        try {
            User user = User.builder()
                    .email(request.getEmail())
                    .password(request.getPassword())
                    .name(request.getName())
                    .phone(request.getPhone())
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
     * @param request 로그인 요청 (email, password)
     * @param response HttpServletResponse (Cookie 설정용)
     * @return 200 OK: 사용자 정보, 401 Unauthorized: 인증 실패
     * 주의: Access Token(1시간), Refresh Token(7일)을 HttpOnly Cookie로 설정하여 XSS 공격 방지
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest request, HttpServletResponse response) {
        try {
            System.out.println("Signin request: " + request);
            User user = userService.signin(request.getEmail(), request.getPassword());
            System.out.println("유저 정보 " + user);
            // JWT 토큰 생성
            String accessToken = jwtUtil.createAccessToken(user.getId());
            String refreshToken = jwtUtil.createRefreshToken(user.getId());
            
            // Refresh Token DB 저장
            RefreshTokenDto refreshTokenEntity = RefreshTokenDto.builder()
                    .userId(user.getId())
                    .token(refreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();
            refreshTokenService.saveRefreshToken(refreshTokenEntity);

            // Refresh Token을 HttpOnly Cookie로 설정 (XSS 방어)
            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
            refreshTokenCookie.setHttpOnly(true);  // JavaScript 접근 차단
            refreshTokenCookie.setSecure(false);   // HTTPS only (개발: false, 운영: true)
            refreshTokenCookie.setPath("/"); // 모든 경로에서 접근 가능 (로그아웃 시 삭제 위해)
            refreshTokenCookie.setMaxAge(60 * 60 * 24); // 1일
            response.addCookie(refreshTokenCookie);

            // Access Token은 응답 Body에 포함
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", accessToken);
            responseData.put("userId", user.getId());
            responseData.put("email", user.getEmail());
            responseData.put("name", user.getName());

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
     * @param request HttpServletRequest (Authorization 헤더에서 AT 추출)
     * @param response HttpServletResponse (RT Cookie 삭제용)
     * @return 200 OK: 로그아웃 성공
     */
    @PostMapping("/signout")
    public ResponseEntity<?> signout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Authorization 헤더에서 Access Token 가져오기
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    int userId = jwtUtil.getUserId(token);
                    // DB에서 Refresh Token 삭제
                    refreshTokenService.deleteByUserId(userId);
                    log.info("로그아웃: userId={} RT 삭제 완료", userId);
                } catch (Exception e) {
                    log.warn("로그아웃 시 AT에서 userId 추출 실패", e);
                }
            }

            // Refresh Token Cookie 삭제
            Cookie refreshTokenCookie = new Cookie("refreshToken", null);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/");
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
                error.put("error", "Refresh Token이 없습니다. 다시 로그인해주세요.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // Refresh Token 검증
            if (!jwtUtil.validateToken(refreshToken)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "유효하지 않은 Refresh 토큰입니다. 다시 로그인해주세요.");
                refreshTokenService.deleteByToken(refreshToken);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // DB에서 토큰 확인
            RefreshTokenDto tokenEntity = refreshTokenService.findByToken(refreshToken);
            if (tokenEntity == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Refresh 토큰이 DB와 일치하지 않습니다. 다시 로그인해주세요.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // 만료 시간 검증
            if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
                // 만료된 토큰은 DB에서 삭제
                refreshTokenService.deleteByToken(refreshToken);
                Map<String, String> error = new HashMap<>();
                error.put("error", "만료된 Refresh 토큰입니다. 다시 로그인해주세요.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
                // Refresh Token rotation: 기존 RT 삭제, 새 RT 발급 및 DB 저장
                log.info("[auth] refresh: rotating RT for userId={}", tokenEntity.getUserId());
                log.debug("[auth] refresh: old RT=[{}]", refreshToken);
                refreshTokenService.deleteByToken(refreshToken);
                log.info("[auth] refresh: deleted old RT");

                String newRefreshToken = jwtUtil.createRefreshToken(tokenEntity.getUserId());
                RefreshTokenDto newTokenEntity = RefreshTokenDto.builder()
                    .userId(tokenEntity.getUserId())
                    .token(newRefreshToken)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();
                refreshTokenService.saveRefreshToken(newTokenEntity);
                log.info("[auth] refresh: saved new RT for userId={}", tokenEntity.getUserId());

                // 새 Refresh Token을 HttpOnly Cookie로 설정
                Cookie refreshTokenCookie = new Cookie("refreshToken", newRefreshToken);
                refreshTokenCookie.setHttpOnly(true);
                refreshTokenCookie.setSecure(false);
                refreshTokenCookie.setPath("/");
                refreshTokenCookie.setMaxAge(60 * 60 * 24); // 1일
                response.addCookie(refreshTokenCookie);

                // 새 Access Token 발급 및 응답 바디로 반환
                String newAccessToken = jwtUtil.createAccessToken(tokenEntity.getUserId());
                log.info("[auth] refresh: issued new accessToken for userId={}", tokenEntity.getUserId());
                Map<String, String> responseData = new HashMap<>();
                responseData.put("accessToken", newAccessToken);
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
            // 요청에서 Access Token 추출 (Authorization 헤더 우선, 없으면 쿠키)
            String token = extractToken(request);
            if (token == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // 토큰 검증 (만료된 경우 ExpiredJwtException 발생)
            if (!jwtUtil.validateToken(token)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "유효하지 않은 Access 토큰입니다.");
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
            
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 토큰 만료 시 401 반환
            log.warn("Access token expired");
            Map<String, String> error = new HashMap<>();
            error.put("error", "토큰이 만료되었습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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
            // 요청에서 Access Token 추출 (Authorization 헤더 우선, 없으면 쿠키)
            String token = extractToken(request);
            if (token == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // 토큰 검증
            if (!jwtUtil.validateToken(token)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "유효하지 않은 토큰입니다.");
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
            
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Access token expired");
            Map<String, String> error = new HashMap<>();
            error.put("error", "토큰이 만료되었습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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
            // 요청에서 Access Token 추출 (Authorization 헤더 우선, 없으면 쿠키)
            String token = extractToken(request);
            if (token == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // 토큰 검증
            if (!jwtUtil.validateToken(token)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "유효하지 않은 토큰입니다.");
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
            
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Access token expired");
            Map<String, String> error = new HashMap<>();
            error.put("error", "토큰이 만료되었습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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

    /**
     * 요청에서 토큰 추출: Authorization 헤더(Bearer) 우선, 없으면 accessToken 쿠키 사용
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return getTokenFromCookie(request, "accessToken");
    }
}
