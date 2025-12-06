package com.ssafy.yumcoach.service;

import com.ssafy.yumcoach.domain.RefreshToken;
import com.ssafy.yumcoach.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    
    private final RefreshTokenMapper refreshTokenMapper;
    
    @Override
    @Transactional
    public void saveRefreshToken(RefreshToken refreshToken) {
        // 기존 토큰 삭제
        deleteByUserId(refreshToken.getUserId());
        
        // 새 토큰 저장
        refreshTokenMapper.insertRefreshToken(refreshToken);
        log.info("RefreshToken saved for userId: {}", refreshToken.getUserId());
    }
    
    @Override
    public RefreshToken findByToken(String token) {
        return refreshTokenMapper.findByToken(token);
    }
    
    @Override
    public RefreshToken findByUserId(Integer userId) {
        return refreshTokenMapper.findByUserId(userId);
    }
    
    @Override
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenMapper.deleteByToken(token);
        log.info("RefreshToken deleted by token");
    }
    
    @Override
    @Transactional
    public void deleteByUserId(Integer userId) {
        refreshTokenMapper.deleteByUserId(userId);
        log.info("RefreshToken deleted for userId: {}", userId);
    }
}
