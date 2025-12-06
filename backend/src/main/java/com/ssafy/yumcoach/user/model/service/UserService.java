package com.ssafy.yumcoach.user.model.service;

import com.ssafy.yumcoach.user.model.User;
import com.ssafy.yumcoach.user.model.UserHealth;

public interface UserService {
    
    /**
     * 회원가입
     */
    void signup(User user);
    
    /**
     * 로그인
     */
    User signin(String email, String password);
    
    /**
     * 이메일로 유저 조회
     */
    User findByEmail(String email);
    
    /**
     * ID로 유저 조회
     */
    User findById(Integer id);
    
    /**
     * 유저 건강정보 조회
     */
    UserHealth findUserHealthByUserId(Integer userId);
    
    /**
     * 유저 건강정보 수정
     */
    void updateUserHealth(UserHealth userHealth);
}
