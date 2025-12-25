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
        if (saved == null) return false;

        boolean ok = saved.equals(inputCode);
        if (ok) redisService.deleteCode(email);
        return ok;
    }
}
