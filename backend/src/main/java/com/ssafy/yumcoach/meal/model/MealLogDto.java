package com.ssafy.yumcoach.meal.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public class MealLogDto {
	private @NonNull Long id;
	private @NonNull int userId;
	private @NonNull String date;
	private String meal; // 아침/점심/저녁/간식
	private String foodCode;
	private Integer gram;
	private double kcal;
	private double protein;
	private double carb;
	private double fat;
}
