package com.ssafy.yumcoach.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
    private Long kakaoId;
    private String email;
    private String nickname;
}
