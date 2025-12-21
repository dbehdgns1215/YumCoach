package com.ssafy.yumcoach.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDietRestriction {
    private Integer id;
    private Integer userId;
    private String restrictionType;
    private String restrictionValue;
}
