package com.ssafy.yumcoach.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHealthRequest {
    private Integer height;
    private Integer weight;
    private Boolean diabetes;
    private Boolean highBloodPressure;
    private Boolean hyperlipidemia;
    private Boolean kidneyDisease;
}
