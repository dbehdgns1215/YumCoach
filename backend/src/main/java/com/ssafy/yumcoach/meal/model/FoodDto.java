package com.ssafy.yumcoach.meal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class FoodDto {
	// SQL DB에서 받아오는 meal id별 정보 디테일
	private @NonNull String foodCode;
	private @NonNull String foodName;
	private @NonNull String category;
	private double weight; // 기본 제공량(g)
	private double energyKcal;
	private double proteinG;
	private double fatG;
	private double carbohydrateG;
}
