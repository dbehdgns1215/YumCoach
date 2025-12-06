package com.ssafy.yumcoach.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenDto {
    private Integer id;
    private Integer userId;
    private String token;
    private LocalDateTime expiresAt;
}
