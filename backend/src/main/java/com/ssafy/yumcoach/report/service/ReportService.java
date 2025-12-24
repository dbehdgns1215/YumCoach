package com.ssafy.yumcoach.report.service;

import com.ssafy.yumcoach.report.model.ReportDto;

import java.time.LocalDate;

public interface ReportService {
    /**
     * 일별 리포트 생성 (사용자 트리거)
     *
     * @param userId 생성 요청자 유저 ID
     * @param date   생성 대상 날짜
     * @return 생성된 ReportDto (PROGRESS 상태 또는 생성 결과)
     */
    ReportDto createDailyReport(int userId, LocalDate date);
    /**
     * 일별 리포트 조회
     *
     * @param userId 조회자 유저 ID
     * @param date   조회할 날짜
     * @return 해당 날짜의 ReportDto 또는 null
     */
    ReportDto getDailyReport(int userId, LocalDate date);
    /**
     * 주간 리포트 생성
     *
     * @param userId   생성 요청자 유저 ID
     * @param fromDate 주간 시작일
     * @param toDate   주간 종료일
     * @return 생성된 ReportDto
     */
    ReportDto createWeeklyReport(int userId, LocalDate fromDate, LocalDate toDate);
    /**
     * 주간 리포트 조회
     *
     * @param userId   조회자 유저 ID
     * @param fromDate 주간 시작일
     * @param toDate   주간 종료일
     * @return 해당 기간의 ReportDto 또는 null
     */
    ReportDto getWeeklyReport(int userId, LocalDate fromDate, LocalDate toDate);
    /**
     * 특정 리포트 ID 조회 (소유자 검증은 호출자에서 수행)
     *
     * @param userId   조회자 유저 ID
     * @param reportId 리포트 ID
     * @return ReportDto 또는 null
     */
    ReportDto getReportById(int userId, int reportId);

    void analyzeReport(int reportId) throws Exception;
}
