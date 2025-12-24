package com.ssafy.yumcoach.challenge.controller;

import com.ssafy.yumcoach.api.response.ApiResponse;
import com.ssafy.yumcoach.challenge.model.ChallengeCreateRequest;
import com.ssafy.yumcoach.challenge.model.ChallengeDto;
import com.ssafy.yumcoach.challenge.model.service.ChallengeService;
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

    /**
     * 챌린지 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createChallenge(
            @RequestHeader("X-USER-ID") Integer userId,
            @RequestBody ChallengeCreateRequest request
    ) {
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
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getChallenges(
            @RequestHeader("X-USER-ID") Integer userId
    ) {
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
    public ResponseEntity<ApiResponse<List<ChallengeDto>>> getActiveChallenges(
            @RequestHeader("X-USER-ID") Integer userId
    ) {
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
            @RequestHeader("X-USER-ID") Integer userId,
            @PathVariable Long challengeId
    ) {
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
            @RequestHeader("X-USER-ID") Integer userId,
            @PathVariable Long challengeId,
            @RequestBody ChallengeCreateRequest request
    ) {
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
            @RequestHeader("X-USER-ID") Integer userId,
            @PathVariable Long challengeId
    ) {
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
            @RequestHeader("X-USER-ID") Integer userId,
            @PathVariable Long challengeId
    ) {
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
    public ResponseEntity<ApiResponse<Void>> toggleChallengeItem(
            @RequestHeader("X-USER-ID") Integer userId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Boolean> payload
    ) {
        log.info("[ChallengeController] toggleChallengeItem itemId={}, done={}", itemId, payload.get("done"));

        try {
            Boolean done = payload.get("done");
            challengeService.toggleChallengeItem(itemId, userId, done);
            return ResponseEntity.ok(ApiResponse.success(null));

        } catch (Exception e) {
            log.error("[ChallengeController] toggleChallengeItem failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("아이템 업데이트 실패"));
        }
    }
}