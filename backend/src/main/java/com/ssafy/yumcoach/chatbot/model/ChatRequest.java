package com.ssafy.yumcoach.chatbot.model;

import lombok.Data;

/**
 * 챗봇 요청 DTO
 * 
 * 클라이언트에서 챗봇 서버로 전송하는 메시지를 담는 객체
 * 
 * @param message 사용자가 입력한 메시지
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
}
