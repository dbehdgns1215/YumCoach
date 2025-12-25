package com.ssafy.yumcoach.auth.service.refresh;

import com.ssafy.yumcoach.auth.model.RefreshTokenDto;

public interface RefreshTokenService {
    
    /**
     * Refresh Token 저장
     */
    void saveRefreshToken(RefreshTokenDto refreshToken);
    
    /**
     * Token으로 조회
     */
    RefreshTokenDto findByToken(String token);
    
    /**
     * UserId로 조회
     */
    RefreshTokenDto findByUserId(Integer userId);
    
    /**
     * Token 삭제
     */
    void deleteByToken(String token);
    
    /**
     * UserId로 삭제
     */
    void deleteByUserId(Integer userId);
}
