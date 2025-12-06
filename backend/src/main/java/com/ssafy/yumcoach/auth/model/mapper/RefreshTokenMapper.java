package com.ssafy.yumcoach.auth.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.yumcoach.auth.model.RefreshTokenDto;

@Mapper
public interface RefreshTokenMapper {
    
    // Refresh Token 저장
    void insertRefreshToken(RefreshTokenDto refreshToken);
    
    // Token으로 조회
    RefreshTokenDto findByToken(@Param("token") String token);
    
    // UserId로 조회
    RefreshTokenDto findByUserId(@Param("userId") Integer userId);
    
    // Token 삭제
    void deleteByToken(@Param("token") String token);
    
    // UserId로 삭제
    void deleteByUserId(@Param("userId") Integer userId);
}
