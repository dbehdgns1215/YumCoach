package com.ssafy.yumcoach.user.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.yumcoach.user.model.User;
import com.ssafy.yumcoach.user.model.UserHealth;

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

    /** 모든 사용자 ID를 반환합니다. 배치/관리 API에서 사용됩니다. */
    java.util.List<Integer> findAllUserIds();

    // 유저 정보 수정
    void updateUser(User user);
    
    // 유저 건강정보 조회
    UserHealth findUserHealthByUserId(@Param("userId") Integer userId);
    
    // 유저 건강정보 수정
    void updateUserHealth(UserHealth userHealth);
}
