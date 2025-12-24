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
public class ChallengeItemDto {
    private Long id;
    private Long challengeId;
    private String text; // itemText -> text for frontend
    private String itemType;
    private LocalDate targetDate;
    private Integer order; // orderIdx -> order
    private Boolean done;
    private LocalDateTime doneAt;
}