package com.ssafy.yumcoach.mapper;

import com.ssafy.yumcoach.domain.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {
    
    // Refresh Token 저장
    void insertRefreshToken(RefreshToken refreshToken);
    
    // Token으로 조회
    RefreshToken findByToken(@Param("token") String token);
    
    // UserId로 조회
    RefreshToken findByUserId(@Param("userId") Integer userId);
    
    // Token 삭제
    void deleteByToken(@Param("token") String token);
    
    // UserId로 삭제
    void deleteByUserId(@Param("userId") Integer userId);
}
