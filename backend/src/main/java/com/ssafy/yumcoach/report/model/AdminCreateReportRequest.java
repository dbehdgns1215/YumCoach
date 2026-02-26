package com.ssafy.yumcoach.report.model;

import lombok.Data;

@Data
public class AdminCreateReportRequest {
    // target user id (required)
    private Integer userId;
    // "DAILY" or "WEEKLY"
    private String type;

    // daily
    private String date;

    // weekly
    private String fromDate;
    private String toDate;
}
