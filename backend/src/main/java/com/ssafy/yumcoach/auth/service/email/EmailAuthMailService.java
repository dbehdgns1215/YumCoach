package com.ssafy.yumcoach.auth.service.email;

public interface EmailAuthMailService {

    /**
     * 인증 코드를 생성해서 이메일로 전송한다.
     *
     * @param email 수신자 이메일
     * @return 생성된 인증 코드 (Redis 저장용)
     */
    String sendAuthCode(String email);
}
