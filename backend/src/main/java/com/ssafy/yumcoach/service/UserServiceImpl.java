package com.ssafy.yumcoach.service;

import com.ssafy.yumcoach.domain.User;
import com.ssafy.yumcoach.domain.UserHealth;
import com.ssafy.yumcoach.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    
    @Override
    @Transactional
    public void signup(User user) {
        // 중복 이메일 체크
        User existingUser = userMapper.findByEmail(user.getEmail());
        if (existingUser != null) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        
        // 회원 등록
        userMapper.insertUser(user);
        
        // 건강정보 초기화
        userMapper.insertUserHealth(user.getId());
        
        log.info("User registered: {}", user.getEmail());
    }
    
    @Override
    public User signin(String email, String password) {
        User user = userMapper.findByEmailAndPassword(email, password);
        if (user == null) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        log.info("User logged in: {}", email);
        return user;
    }
    
    @Override
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }
    
    @Override
    public User findById(Integer id) {
        return userMapper.findById(id);
    }
    
    @Override
    public UserHealth findUserHealthByUserId(Integer userId) {
        UserHealth userHealth = userMapper.findUserHealthByUserId(userId);
        if (userHealth == null) {
            log.warn("UserHealth not found for userId: {}", userId);
        }
        return userHealth;
    }
    
    @Override
    @Transactional
    public void updateUserHealth(UserHealth userHealth) {
        userMapper.updateUserHealth(userHealth);
        log.info("UserHealth updated for userId: {}", userHealth.getUserId());
    }
}
