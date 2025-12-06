package com.ssafy.yumcoach.service;

import com.ssafy.yumcoach.domain.RefreshToken;

public interface RefreshTokenService {
    
    /**
     * Refresh Token 저장
     */
    void saveRefreshToken(RefreshToken refreshToken);
    
    /**
     * Token으로 조회
     */
    RefreshToken findByToken(String token);
    
    /**
     * UserId로 조회
     */
    RefreshToken findByUserId(Integer userId);
    
    /**
     * Token 삭제
     */
    void deleteByToken(String token);
    
    /**
     * UserId로 삭제
     */
    void deleteByUserId(Integer userId);
}
