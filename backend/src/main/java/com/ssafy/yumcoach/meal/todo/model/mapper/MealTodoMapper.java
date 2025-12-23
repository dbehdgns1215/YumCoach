package com.ssafy.yumcoach.meal.todo.model.mapper;

import com.ssafy.yumcoach.meal.enums.MealType;
import com.ssafy.yumcoach.meal.todo.model.MealTodoDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MealTodoMapper {

    MealTodoDto findById(
            @Param("id") Long id,
            @Param("userId") Integer userId
    );

    List<MealTodoDto> findByUser(
            @Param("userId") Integer userId
    );

    List<MealTodoDto> findByUserAndMealType(
            @Param("userId") Integer userId,
            @Param("mealType") MealType mealType
    );

    void insert(MealTodoDto todo);

    void deleteById(
            @Param("id") Long id,
            @Param("userId") Integer userId
    );
}
