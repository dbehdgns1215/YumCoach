package com.ssafy.yumcoach.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KakaoUserResponse {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Data
    public static class KakaoAccount {
        private String email;

        @JsonProperty("profile")
        private Profile profile;
    }

    @Data
    public static class Profile {
        private String nickname;
    }
}
