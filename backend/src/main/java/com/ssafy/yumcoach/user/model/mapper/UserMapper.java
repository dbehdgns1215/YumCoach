package com.ssafy.yumcoach.user.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.yumcoach.user.model.User;
import com.ssafy.yumcoach.user.model.UserHealth;

import java.util.List;

@Mapper
public interface UserMapper {

    // 회원가입
    void insertUser(User user);

    // 유저 건강정보 초기화
    void insertUserHealth(Integer userId);

    // 로그인용 - 이메일과 비밀번호로 유저 조회
    User findByEmailAndPassword(@Param("email") String email, @Param("password") String password);

    // 이메일로 유저 조회
    User findByEmail(@Param("email") String email);

    // ID로 유저 조회
    User findById(@Param("id") Integer id);

    List<Integer> findAllUserIds();

    // 유저 정보 수정
    void updateUser(User user);

    // 유저 건강정보 조회
    UserHealth findUserHealthByUserId(@Param("userId") Integer userId);

    // 유저 건강정보 수정
    void updateUserHealth(UserHealth userHealth);

    // 역할 변경
    void updateUserRole(@Param("id") Integer id, @Param("role") String role);
}
