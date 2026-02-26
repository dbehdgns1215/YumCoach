package com.ssafy.yumcoach.report.model.mapper;

import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.model.ReportMealDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import com.ssafy.yumcoach.report.model.ReportInsightDto;

@Mapper
public interface ReportMapper {
    /**
     * report 테이블에 리포트 요약을 저장합니다.
     * - 입력 후 `report.id`에 생성된 PK가 채워져야 합니다 (MyBatis `useGeneratedKeys`).
     */
    int insertReport(ReportDto report);

    /**
     * report_meal 테이블에 리포트 내 개별 식사 요약을 저장합니다.
     */
    int insertReportMeal(ReportMealDto meal);

    /**
     * 리포트 ID로 조회
     */
    ReportDto selectReportById(@Param("id") Integer id);

    /**
     * 유저/타입/일자 기준 리포트 조회 (일별 사용)
     */
    ReportDto selectReportByUserTypeDate(@Param("userId") Integer userId, @Param("type") String type, @Param("date") LocalDate date);

    /**
     * 유저/타입/범위 기준 리포트 조회 (주간 사용)
     */
    ReportDto selectReportByUserAndRange(@Param("userId") Integer userId, @Param("type") String type, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    /**
     * 리포트 생성 시도를 로그로 남깁니다. (LIMIT_EXCEEDED, CREATED, FAILED 등)
     */
    int insertGenerationLog(@Param("userId") Integer userId,
                            @Param("type") String type,
                            @Param("date") LocalDate date,
                            @Param("fromDate") LocalDate fromDate,
                            @Param("toDate") LocalDate toDate,
                            @Param("triggeredBy") String triggeredBy,
                            @Param("result") String result,
                            @Param("reportId") Integer reportId,
                            @Param("details") String details);

    /**
     * 특정 기간 내 생성 로그 건수를 셉니다. 생성 제한 정책 검사에 사용됩니다.
     */
    int countGenerationLogsInPeriod(@Param("userId") Integer userId,
                                    @Param("type") String type,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("triggeredBy") String triggeredBy);

    /**
     * user_generation_count 테이블에 삽입하거나 업데이트합니다.
     * - 배치가 전체 사용자에 대해 일별/주별 집계를 계산한 후 이 메서드를 호출합니다.
     */
    int upsertUserGenerationCount(@Param("userId") Integer userId,
                                 @Param("dailyDate") LocalDate dailyDate,
                                 @Param("dailyUsed") Integer dailyUsed,
                                 @Param("weeklyFrom") LocalDate weeklyFrom,
                                 @Param("weeklyUsed") Integer weeklyUsed);

    /** 조회: user_generation_count 레코드 조회 (user_id 기준, 최신 하나) */
    Map<String, Object> selectUserGenerationCount(@Param("userId") Integer userId);

    /**
     * report.ai_response 컬럼에 AI 원문(JSON)을 저장합니다.
     */
    int updateReportAiResponse(@Param("reportId") Integer reportId, @Param("aiResponse") String aiResponse);

    /** 리포트 요약(영양소 합계/식사수) 업데이트 */
    int updateReportSummary(@Param("reportId") Integer reportId,
                            @Param("totalCalories") Integer totalCalories,
                            @Param("proteinG") Integer proteinG,
                            @Param("carbG") Integer carbG,
                            @Param("fatG") Integer fatG,
                            @Param("mealCount") Integer mealCount);

    /**
     * report_insight 테이블에 AI가 생성한 인사이트를 저장합니다.
     */
    int insertReportInsight(@Param("reportId") Integer reportId, @Param("kind") String kind, @Param("title") String title, @Param("body") String body);

    /** 조회한 리포트의 인사이트 목록을 반환합니다. */
    java.util.List<ReportInsightDto> selectReportInsights(@Param("reportId") Integer reportId);

    /** 리포트에 포함된 meal 목록 조회 */
    java.util.List<ReportMealDto> selectReportMeals(@Param("reportId") Integer reportId);

    /** 리포트 코치 메시지 업데이트 */
    void updateReportCoachMessage(@Param("reportId") int reportId, @Param("coachMessage") String coachMessage);
    
    /** 리포트 다음 행동 업데이트 */
    void updateReportNextAction(@Param("reportId") int reportId, @Param("nextAction") String nextAction);

    /** 스케줄러용: 생성된 리포트의 상태와 생성자(created_by)를 업데이트합니다. */
    void updateReportStatusCreatedBy(@Param("reportId") Integer reportId, @Param("status") String status, @Param("createdBy") String createdBy);

    /** 리포트 점수 업데이트 */
    void updateReportScore(@Param("reportId") int reportId, @Param("score") Integer score);

    /** 리포트 요약(헤로) 업데이트 */
    void updateReportHero(@Param("reportId") int reportId, @Param("heroTitle") String heroTitle, @Param("heroLine") String heroLine);

    void deleteInsightsByReportId(@Param("reportId") int reportId);
}
