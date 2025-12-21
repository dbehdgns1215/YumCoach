package com.ssafy.yumcoach.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHealth {
    private Integer id;
    private Integer userId;
    private Integer height; // cm
    private Integer weight; // kg
    private Boolean diabetes;
    private Boolean highBloodPressure;
    private Boolean hyperlipidemia;
    private Boolean kidneyDisease;
    private String activityLevel;
}
