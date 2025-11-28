package com.ssafy.yumcoach.meal.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MealDto {
	private Integer dietLogItemId; // PK
	private Integer dietLogId; // FK - diet_logs.diet_log_id
	private String foodCode; // VARCHAR(50)
	private Double servingSize; // DECIMAL(5,2)
	private java.time.LocalDateTime createdAt;
	private java.time.LocalDateTime updatedAt;
}
