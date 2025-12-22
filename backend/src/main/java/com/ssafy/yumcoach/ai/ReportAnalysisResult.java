package com.ssafy.yumcoach.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 분석 결과 DTO
 *
 * 프론트엔드에서 기대하는 구조를 간단히 표현합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAnalysisResult {
    private String heroTitle;
    private String heroLine;
    private Integer score;
    private String coachMessage;
    private String nextAction;

    public static class Insight {
        private String kind; // good | warn | keep
        private String title;
        private String body;

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    private List<Insight> insights;
}
