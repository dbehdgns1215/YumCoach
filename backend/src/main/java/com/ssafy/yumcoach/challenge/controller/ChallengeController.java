package com.ssafy.yumcoach.challenge.controller;

import com.ssafy.yumcoach.api.response.ApiResponse;
import com.ssafy.yumcoach.challenge.model.ChallengeCreateRequest;
import com.ssafy.yumcoach.challenge.model.ChallengeDto;
import com.ssafy.yumcoach.challenge.model.service.ChallengeService;
import com.ssafy.yumcoach.auth.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@Slf4j
public class ChallengeController {

    private final ChallengeService challengeService;
    private final JwtUtil jwtUtil;

    /**
     * 챌린지 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createChallenge(
            HttpServletRequest httpRequest,
            @RequestBody ChallengeCreateRequest request
    ) {
        // 🔥 헬퍼 메서드 사용
        Integer userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }
        
        log.info("[ChallengeController] createChallenge userId={}, request={}", userId, request);

        try {
            Long challengeId = challengeService.createChallenge(userId, request);

            Map<String, Object> data = new HashMap<>();
            data.put("challengeId", challengeId);

            return ResponseEntity.ok(ApiResponse.success(data));

        } catch (Exception e) {
            log.error("[ChallengeController] createChallenge failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("챌린지 생성 실패: " + e.getMessage()));
        }
    }

    /**
     * 챌린지 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getChallenges(HttpServletRequest httpRequest) {
        Integer userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }
        
        log.debug("[ChallengeController] getChallenges userId={}", userId);
        
        try {
            List<ChallengeDto> challenges = challengeService.getChallengesByUserId(userId);
            return ResponseEntity.ok(ApiResponse.success(challenges));
        } catch (Exception e) {
            log.error("[ChallengeController] getChallenges failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("챌린지 조회 실패"));
        }
    }

    /**
     * 활성 챌린지 조회 (리포트/챗봇용)
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getActiveChallenges(HttpServletRequest httpRequest) {
        Integer userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }
        
        log.debug("[ChallengeController] getActiveChallenges userId={}", userId);
        
        try {
            List<ChallengeDto> challenges = challengeService.getActiveChallenges(userId);
            return ResponseEntity.ok(ApiResponse.success(challenges));
        } catch (Exception e) {
            log.error("[ChallengeController] getActiveChallenges failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("활성 챌린지 조회 실패"));
        }
    }

    /**
     * 챌린지 상세 조회
     */
    @GetMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<ChallengeDto>> getChallengeById(
            HttpServletRequest httpRequest,
            @PathVariable Long challengeId
    ) {
        Integer userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }
        
        log.debug("[ChallengeController] getChallengeById challengeId={}, userId={}", challengeId, userId);
        
        try {
            ChallengeDto challenge = challengeService.getChallengeById(challengeId, userId);
            return ResponseEntity.ok(ApiResponse.success(challenge));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[ChallengeController] getChallengeById failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("챌린지 조회 실패"));
        }
    }

    /**
     * 챌린지 수정
     */
    @PutMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<Void>> updateChallenge(
            HttpServletRequest httpRequest,
            @PathVariable Long challengeId,
            @RequestBody ChallengeCreateRequest request
    ) {
        Integer userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }
        
        log.info("[ChallengeController] updateChallenge challengeId={}, userId={}", challengeId, userId);
        
        try {
            challengeService.updateChallenge(challengeId, userId, request);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[ChallengeController] updateChallenge failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("챌린지 수정 실패"));
        }
    }

    /**
     * 챌린지 완료 처리
     */
    @PatchMapping("/{challengeId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeChallenge(
            HttpServletRequest httpRequest,
            @PathVariable Long challengeId
    ) {
        Integer userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }
        
        log.info("[ChallengeController] completeChallenge challengeId={}, userId={}", challengeId, userId);
        
        try {
            challengeService.completeChallenge(challengeId, userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[ChallengeController] completeChallenge failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("챌린지 완료 처리 실패"));
        }
    }

    /**
     * 챌린지 삭제
     */
    @DeleteMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(
            HttpServletRequest httpRequest,
            @PathVariable Long challengeId
    ) {
        Integer userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }
        
        log.info("[ChallengeController] deleteChallenge challengeId={}, userId={}", challengeId, userId);
        
        try {
            challengeService.deleteChallenge(challengeId, userId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[ChallengeController] deleteChallenge failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("챌린지 삭제 실패"));
        }
    }

    /**
     * 챌린지 아이템 토글
     */
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<ChallengeDto>> toggleChallengeItem(
            HttpServletRequest httpRequest,
            @PathVariable Long itemId,
            @RequestBody Map<String, Boolean> payload
    ) {
        Integer userId = extractUserId(httpRequest);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증이 필요합니다."));
        }

        log.info("[ChallengeController] toggleChallengeItem itemId={}, userId={}, done={}",
                itemId, userId, payload.get("done"));

        try {
            Boolean done = payload.get("done");
            challengeService.toggleChallengeItem(itemId, userId, done);

            // 반환: 변경된 챌린지 상태(진척도 포함)를 바로 내려줘서 프론트가 갱신할 수 있게 함
            ChallengeDto updated = challengeService.getChallengeById(challengeService.selectChallengeIdByItemId(itemId), userId);
            return ResponseEntity.ok(ApiResponse.success(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[ChallengeController] toggleChallengeItem failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("아이템 업데이트 실패"));
        }
    }

    // ===== Private Helper Methods =====

    /**
     * 🔥 요청에서 userId 추출 (Authorization 헤더 또는 쿠키에서 토큰 추출)
     */
    private Integer extractUserId(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                log.warn("[ChallengeController] extractUserId: missing or invalid token");
                return null;
            }
            return jwtUtil.getUserId(token);
        } catch (Exception e) {
            log.warn("[ChallengeController] extractUserId: failed to extract userId", e);
            return null;
        }
    }

    /**
     * 요청에서 JWT 토큰 추출 (Authorization 우선, 없으면 accessToken 쿠키)
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("accessToken".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}