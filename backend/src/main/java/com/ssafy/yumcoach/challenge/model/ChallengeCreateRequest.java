package com.ssafy.yumcoach.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeCreateRequest {
    private String title;
    private String description;

    @JsonProperty("goalType")
    private String goalType;

    @JsonProperty("goalDetails")
    private Map<String, Object> goalDetails;

    @JsonProperty("startDate")
    private String startDate; // "2025-01-01" 형식

    @JsonProperty("durationDays")
    private Integer durationDays;

    private String source; // CHATBOT, REPORT, MANUAL

    @JsonProperty("sourceId")
    private Long sourceId;

    private List<ItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {
        private String text;
        private Integer order;
    }
}