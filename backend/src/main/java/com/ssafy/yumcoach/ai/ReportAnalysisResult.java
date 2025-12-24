package com.ssafy.yumcoach.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 분석 결과 DTO
 *
 * 프론트엔드에서 기대하는 구조를 간단히 표현합니다.
 * Jackson 어노테이션을 추가해 camelCase 또는 snake_case 모두 매핑되도록 합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAnalysisResult {
    @JsonAlias({"heroTitle", "hero_title"})
    private String heroTitle;

    @JsonAlias({"heroLine", "hero_line"})
    private String heroLine;

    @JsonAlias({"score"})
    private Integer score;

    @JsonAlias({"coachMessage", "coach_message"})
    private String coachMessage;

    @JsonAlias({"nextAction", "next_action"})
    private String nextAction;

    public static class Insight {
        @JsonAlias({"kind"})
        private String kind; // good | warn | keep

        @JsonAlias({"title"})
        private String title;

        @JsonAlias({"body"})
        private String body;

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    @JsonAlias({"insights"})
    private List<Insight> insights;
}
