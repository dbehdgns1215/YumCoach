package com.ssafy.yumcoach.challenge.model.mapper;

import com.ssafy.yumcoach.challenge.model.Challenge;
import com.ssafy.yumcoach.challenge.model.ChallengeDailyLog;
import com.ssafy.yumcoach.challenge.model.ChallengeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ChallengeMapper {

    // ===== Challenge =====
    void insertChallenge(Challenge challenge);

    Challenge selectChallengeById(@Param("id") Long id);

    List<Challenge> selectChallengesByUserId(@Param("userId") Integer userId);

    List<Challenge> selectActiveChallengesByUserId(@Param("userId") Integer userId);

    ChallengeItem selectItemById(@Param("id") Long id);

    void updateChallenge(Challenge challenge);

    void updateChallengeProgress(@Param("id") Long id,
                                 @Param("currentStreak") Integer currentStreak,
                                 @Param("maxStreak") Integer maxStreak,
                                 @Param("totalSuccessDays") Integer totalSuccessDays,
                                 @Param("achievementRate") Double achievementRate,
                                 @Param("progressRate") Double progressRate);

    void updateChallengeStatus(@Param("id") Long id, @Param("status") String status);

    void deleteChallenge(@Param("id") Long id);

    // ===== ChallengeItem =====
    void insertChallengeItem(ChallengeItem item);

    List<ChallengeItem> selectItemsByChallengeId(@Param("challengeId") Long challengeId);

    void updateChallengeItem(ChallengeItem item);

    void deleteChallengeItemsByChallengeId(@Param("challengeId") Long challengeId);

    // ===== ChallengeDailyLog =====
    void insertDailyLog(ChallengeDailyLog log);

    ChallengeDailyLog selectDailyLog(@Param("challengeId") Long challengeId,
                                     @Param("logDate") LocalDate logDate);

    List<ChallengeDailyLog> selectRecentLogs(@Param("challengeId") Long challengeId,
                                             @Param("days") Integer days);

    void updateDailyLog(ChallengeDailyLog log);

    void deleteDailyLogsByChallengeId(@Param("challengeId") Long challengeId);
}