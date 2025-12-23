package com.ssafy.yumcoach.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportInsightDto {
    private Integer id;
    private Integer reportId;
    private String kind; // good | warn | keep | coach | action
    private String title;
    private String body;
}
