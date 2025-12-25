package com.ssafy.yumcoach.auth.service.email;

public interface EmailAuthRedisService {

    /**
     * 이메일 인증 코드 저장 (TTL 포함)
     */
    void saveCode(String email, String code);

    /**
     * 이메일에 해당하는 인증 코드 조회
     */
    String getCode(String email);

    /**
     * 이메일 인증 코드 삭제
     */
    void deleteCode(String email);

    /**
     * 이메일 인증 성공 플래그 저장 (일정 TTL)
     */
    void saveVerified(String email);

    /**
     * 이메일이 인증되었는지 여부 확인
     */
    boolean isVerified(String email);
}
