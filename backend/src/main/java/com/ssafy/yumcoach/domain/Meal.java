package com.ssafy.yumcoach.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Meal {
    private Integer id;
    private Integer historyId;
    private String mealCode;
    private String mealName;
    private Integer amount;
}
