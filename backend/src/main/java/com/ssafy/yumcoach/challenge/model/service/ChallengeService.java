package com.ssafy.yumcoach.challenge.model.service;

import com.ssafy.yumcoach.challenge.model.ChallengeCreateRequest;
import com.ssafy.yumcoach.challenge.model.ChallengeDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ChallengeService {

    /**
     * 챌린지 생성
     */
    Long createChallenge(Integer userId, ChallengeCreateRequest request);

    /**
     * 챌린지 상세 조회
     */
    ChallengeDto getChallengeById(Long challengeId, Integer userId);

    /**
     * 사용자의 모든 챌린지 조회
     */
    List<ChallengeDto> getChallengesByUserId(Integer userId);

    /**
     * 사용자의 활성 챌린지 조회 (리포트/챗봇 연동용)
     */
    List<ChallengeDto> getActiveChallenges(Integer userId);

    /**
     * 챌린지 수정
     */
    void updateChallenge(Long challengeId, Integer userId, ChallengeCreateRequest request);

    /**
     * 챌린지 완료 처리
     */
    void completeChallenge(Long challengeId, Integer userId);

    /**
     * 챌린지 삭제
     */
    void deleteChallenge(Long challengeId, Integer userId);

    /**
     * 챌린지 아이템 완료 토글
     */
    void toggleChallengeItem(Long itemId, Integer userId, Boolean done);

    /**
     * 아이템 id로 챌린지 id 조회 (도움 메서드)
     */
    Long selectChallengeIdByItemId(Long itemId);

    /**
     * 일일 로그 기록 (리포트에서 자동 호출)
     */
    void recordDailyLog(Long challengeId, LocalDate logDate, Map<String, Object> reportData);

    /**
     * 챌린지 진척도 업데이트 (내부용)
     */
    void updateChallengeProgress(Long challengeId);
}