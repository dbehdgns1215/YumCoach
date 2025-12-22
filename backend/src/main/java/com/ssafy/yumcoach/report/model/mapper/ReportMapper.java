package com.ssafy.yumcoach.report.model.mapper;

import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.model.ReportMealDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

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

}
