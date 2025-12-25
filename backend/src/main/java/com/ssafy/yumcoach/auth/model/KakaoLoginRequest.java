package com.ssafy.yumcoach.auth.model;

import lombok.Data;

@Data
public class KakaoLoginRequest {
    private String code;
    private String redirectUri;
}
