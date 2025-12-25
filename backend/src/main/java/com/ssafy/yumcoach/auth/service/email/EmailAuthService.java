package com.ssafy.yumcoach.auth.service.email;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final EmailAuthMailService mailService;
    private final EmailAuthRedisService redisService;

    public void sendCode(String email) {
        String code = mailService.sendAuthCode(email);
        redisService.saveCode(email, code);
    }

    public boolean verifyCode(String email, String inputCode) {
        String saved = redisService.getCode(email);
        if (saved == null)
            return false;

        boolean ok = saved.equals(inputCode);
        if (ok) {
            // 인증 성공 시 verified 플래그 저장 후 코드 제거
            redisService.saveVerified(email);
            redisService.deleteCode(email);
        }
        return ok;
    }

    public boolean isVerified(String email) {
        return redisService.isVerified(email);
    }
}
