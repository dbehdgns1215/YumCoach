package com.ssafy.yumcoach.report.model;

import lombok.Data;

@Data
public class CreateReportRequest {
    // For daily: date (YYYY-MM-DD). For weekly: fromDate/toDate or weekStart
    /**
     * 클라이언트에서 리포트 생성 요청 시 사용하는 DTO
     *
     * - `date`: 일별 리포트를 생성할 날짜 문자열(ISO yyyy-MM-dd)
     * - `fromDate`/`toDate`: 주간 리포트의 기간(ISO 문자열)
     *
     * 서버에서 파싱하여 적절한 날짜로 변환합니다.
     */
    private String date;
    private String fromDate;
    private String toDate;
}
