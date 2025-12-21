package com.ssafy.yumcoach.user.model.mapper;

import com.ssafy.yumcoach.user.model.UserDietRestriction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserDietRestrictionMapper {
    List<UserDietRestriction> findByUserId(@Param("userId") Integer userId);
    void deleteByUserId(@Param("userId") Integer userId);
    void insertRestriction(UserDietRestriction restriction);
}
