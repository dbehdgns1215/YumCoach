package com.ssafy.yumcoach.user.model.service;

import com.ssafy.yumcoach.user.model.User;
import com.ssafy.yumcoach.user.model.UserHealth;
import com.ssafy.yumcoach.user.model.UserDietRestriction;
import java.util.List;

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
     * 유저 정보 수정
     */
    void updateUser(User user);

    /**
     * 유저 식이제한 조회
     */
    java.util.List<UserDietRestriction> findUserDietRestrictionsByUserId(Integer userId);

    /**
     * 유저 식이제한 전체 업데이트(기존 삭제 후 삽입)
     */
    void updateUserDietRestrictions(Integer userId, java.util.List<UserDietRestriction> restrictions);

    /**
     * 유저 건강정보 수정
     */
    void updateUserHealth(UserHealth userHealth);

    /**
     * 유저 권한/등급 변경
     */
    void updateUserRole(Integer id, String role);
}
