package com.ssafy.yumcoach.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드는 JSON에서 제외
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ApiError error;
    private LocalDateTime timestamp;

    // ===== 성공 응답 만들기 =====
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                data,
                null,
                LocalDateTime.now()
        );
    }

    // ===== 실패 응답 만들기 =====
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(
                false,
                null,
                new ApiError(code, message),
                LocalDateTime.now()
        );
    }

    // ===== 에러만 간단하게 =====
    public static <T> ApiResponse<T> error(String message) {
        return error("ERROR", message);
    }

    // ===== 에러 정보 클래스 =====
    @Getter
    @AllArgsConstructor
    public static class ApiError {
        private String code;     // 에러 코드 (예: USER_NOT_FOUND)
        private String message;  // 에러 메시지 (예: 유저를 찾을 수 없습니다)
    }
}
