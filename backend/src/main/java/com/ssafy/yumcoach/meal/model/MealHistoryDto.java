package com.ssafy.yumcoach.meal.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;

@Data
@Setter
@ToString
@Builder
public class MealHistoryDto {
	private int id;
	private int userId;
	private String createdDate; // 히스토리가 생성된 일자
	private String date; // 식사한 일자
	private String type; // 아침, 점심, 저녁, (간식, 야식)

	@Builder.Default
	private List<MealDto> mealList; // 식사

	public MealHistoryDto(int id, int userId, String createdDate, String date, String type, List<MealDto> meal) {
		this.userId = userId;
		this.createdDate = createdDate;
		this.date = date;
		this.type = type;
		this.mealList = meal;
	}

}
