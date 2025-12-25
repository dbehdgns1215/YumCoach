package com.ssafy.yumcoach.auth.controller;

import com.ssafy.yumcoach.auth.model.EmailSendDto;
import com.ssafy.yumcoach.auth.model.EmailVerifyDto;
import com.ssafy.yumcoach.auth.service.email.EmailAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/email")
public class EmailAuthController {

    private final EmailAuthService emailAuthService;

    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestBody @jakarta.validation.Valid EmailSendDto req) {
        emailAuthService.sendCode(req.email());
        return ResponseEntity.ok().body(java.util.Map.of("message", "인증번호를 전송했습니다."));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody @jakarta.validation.Valid EmailVerifyDto req) {
        boolean ok = emailAuthService.verifyCode(req.email(), req.code());
        if (!ok) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "인증번호가 올바르지 않거나 만료되었습니다."));
        }
        return ResponseEntity.ok().body(java.util.Map.of("message", "인증 성공"));
    }
}
