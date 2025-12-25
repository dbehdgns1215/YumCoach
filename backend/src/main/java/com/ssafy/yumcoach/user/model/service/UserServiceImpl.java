package com.ssafy.yumcoach.user.model.service;

import com.ssafy.yumcoach.user.model.User;
import com.ssafy.yumcoach.user.model.UserHealth;
import com.ssafy.yumcoach.user.model.UserDietRestriction;
import com.ssafy.yumcoach.user.model.mapper.UserMapper;
import com.ssafy.yumcoach.user.model.mapper.UserDietRestrictionMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserDietRestrictionMapper restrictionMapper;

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
    public User findByKakaoId(Long kakaoId) {
        return userMapper.findByKakaoId(kakaoId);
    }

    @Override
    @Transactional
    public void signupSocial(User user) {
        // 카카오 ID 중복 체크
        if (user.getKakaoId() != null) {
            User existingByKakao = userMapper.findByKakaoId(user.getKakaoId());
            if (existingByKakao != null) {
                throw new IllegalArgumentException("이미 등록된 카카오 계정입니다.");
            }
        }

        // 이메일 중복 체크 (선택)
        if (user.getEmail() != null) {
            User existingByEmail = userMapper.findByEmail(user.getEmail());
            if (existingByEmail != null) {
                throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
            }
        }

        // 회원 등록 (password는 null 가능)
        userMapper.insertUser(user);

        // 건강정보 초기화
        userMapper.insertUserHealth(user.getId());

        log.info("Social user registered: kakaoId={}, email={}", user.getKakaoId(), user.getEmail());
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        userMapper.updateUser(user);
        log.info("User updated: {}", user.getId());
    }

    @Override
    public java.util.List<UserDietRestriction> findUserDietRestrictionsByUserId(Integer userId) {
        return restrictionMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public void updateUserDietRestrictions(Integer userId, java.util.List<UserDietRestriction> restrictions) {
        // 전체 삭제 후 재삽입
        restrictionMapper.deleteByUserId(userId);
        if (restrictions != null) {
            for (UserDietRestriction r : restrictions) {
                r.setUserId(userId);
                restrictionMapper.insertRestriction(r);
            }
        }
        log.info("UserDietRestrictions updated for userId={}", userId);
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

    @Override
    @Transactional
    public void updateUserRole(Integer id, String role) {
        userMapper.updateUserRole(id, role);
        log.info("User role updated: id={}, role={}", id, role);
    }
}
