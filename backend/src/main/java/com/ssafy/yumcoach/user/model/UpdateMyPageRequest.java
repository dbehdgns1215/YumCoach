package com.ssafy.yumcoach.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMyPageRequest {
    // User fields
    private String name;
    private String nickname;
    private String phone;
    private String gender;
    private Integer age;

    // Health fields
    private Integer height;
    private Integer weight;
    private Boolean diabetes;
    private Boolean highBloodPressure;
    private Boolean hyperlipidemia;
    private Boolean kidneyDisease;
    private Integer activityLevel;

    // Diet restrictions
    private List<UserDietRestriction> dietRestrictions;
}
