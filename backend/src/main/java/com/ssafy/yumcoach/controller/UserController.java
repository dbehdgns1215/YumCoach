package com.ssafy.yumcoach.controller;

import com.ssafy.yumcoach.controller.dto.*;
import com.ssafy.yumcoach.domain.RefreshToken;
import com.ssafy.yumcoach.domain.User;
import com.ssafy.yumcoach.domain.UserHealth;
import com.ssafy.yumcoach.service.RefreshTokenService;
import com.ssafy.yumcoach.service.UserService;
import com.ssafy.yumcoach.util.JwtUtil;
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
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest request) {
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
            
            // 응답
            TokenResponse response = TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .build();
            
            return ResponseEntity.ok(response);
            
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
     */
    @PostMapping("/signout")
    public ResponseEntity<?> signout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            int userId = jwtUtil.getUserId(token);
            
            // Refresh Token 삭제
            refreshTokenService.deleteByUserId(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "로그아웃 되었습니다.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Signout error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "로그아웃 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            
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
            
            // 새 Access Token 발급
            String newAccessToken = jwtUtil.createAccessToken(tokenEntity.getUserId());
            
            Map<String, String> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Token refresh error", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "토큰 갱신 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
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
     */
    @GetMapping("/health")
    public ResponseEntity<?> getUserHealth(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
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
     */
    @PutMapping("/health")
    public ResponseEntity<?> updateUserHealth(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateHealthRequest request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            int userId = jwtUtil.getUserId(token);
            
            UserHealth userHealth = UserHealth.builder()
                    .userId(userId)
                    .height(request.getHeight())
                    .weight(request.getWeight())
                    .diabetes(request.getDiabetes())
                    .highBloodPressure(request.getHighBloodPressure())
                    .hyperlipidemia(request.getHyperlipidemia())
                    .kidneyDisease(request.getKidneyDisease())
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
}
