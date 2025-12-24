package com.ssafy.yumcoach.challenge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeItem {
    private Long id;
    private Long challengeId;

    private String itemText;
    private String itemType; // ACTION, TIP, MILESTONE
    private LocalDate targetDate;

    private Integer orderIdx;
    private Boolean done;
    private LocalDateTime doneAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}