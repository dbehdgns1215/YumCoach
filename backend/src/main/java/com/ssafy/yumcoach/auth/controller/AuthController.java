package com.ssafy.yumcoach.auth.controller;

import com.ssafy.yumcoach.auth.model.AuthResponse;
import com.ssafy.yumcoach.auth.model.KakaoLoginRequest;
import com.ssafy.yumcoach.auth.service.kakao.KakaoAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;

    @PostMapping("/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestBody KakaoLoginRequest request) {

        AuthResponse response =
                kakaoAuthService.login(request.getCode(), request.getRedirectUri());

        return ResponseEntity.ok(response);
    }
}
