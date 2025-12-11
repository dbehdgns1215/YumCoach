package com.ssafy.yumcoach.meal.model;

import com.ssafy.yumcoach.meal.enums.MealType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 한 끼 단위
// meal_history 테이블
public class MealLogDto implements Serializable {

	private Long id; // dietLogId 느낌
	private Long userId;
	private LocalDate date; // meal_history.date
	private MealType mealType; // 아침/점심/저녁/간식 (enum) meal_history.type

	// 이 끼니에 포함된 음식 아이템들
	private List<MealItemDto> items;

	// 합계 정보, 프론트에서도 가능
	// private double totalKcal;
	// private double totalProtein;
	// private double totalCarb;
	// private double totalFat;
}
